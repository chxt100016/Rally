# 支付域设计方案（MVP：活动后收款 + 分账 + 对账）

目标读者：后端。前端为微信小程序。遵循 COLA 5.0 分层与 DDD 约定。支付域为独立限界上下文，不反向依赖约球域，便于后续扩展退款 / 预收 / 多渠道 / 其它业务。

---

## 1. MVP 范围与流程

**核心改动：后付费模式。** 报名流程完全不变；费用在**活动结束后**由发起人在详情页「发起收款」，参与者再支付，平台收款后分账给发起人。

后付费天然消除了预收、多退少补、退款的复杂度——因为没有「提前收的钱」需要退。实到人数在收款时已确定，金额一次算准。

```
活动结束(FINISHED)
   │
   ▼  发起人在详情页「发起收款」（选应付人 + 输入总额/每人金额）
[收款批次] 为每个应付参与者生成 1 笔 payment_order(PENDING)
   │
   ▼  参与者各自微信支付
微信回调 → payment_order=PAID
   │
   ▼  支付成功后对该笔交易发起分账
payment_settlement → 微信分账 → 查询/回调 → FINISHED（钱到发起人）
   │
   ▼  对账 Job 兜底（超时关单 / 分账结果查询）
```

### 1.1 MVP 做什么 / 不做什么

| 能力 | MVP | 说明 |
|---|---|---|
| 发起收款（活动后） | ✅ | 发起人详情页触发，生成支付单 |
| 微信支付 + 回调 | ✅ | JSAPI 下单、异步回调验签、幂等推进 |
| 分账给发起人 | ✅ | 按笔交易分账，个人 openid 接收方 |
| 支付流水 / 对账 | ✅ | `payment_order` 流水 + 查单 + 回调留痕 + 对账 Job |
| 手续费千 6 展示 | ✅ | 实付 = 本金 + ceil(本金×0.6%)，可配置 |
| 退款 | ❌ | 后付费无预收，无需退款（误收等走人工，后续迭代） |
| 报名预收 / 多退少补 | ❌ | 不改报名流程，不引入待支付状态 |
| 多渠道（支付宝等） | 🔭 | 接口已抽象，MVP 仅微信 |

---

## 2. 领域划分

```
约球域(meetup) ──仅提供「活动是否结束/参与者名单/发起人」──► 支付域(payment) ──gateway──► 微信支付(WechatPay)
```
- 支付域只认 `bizType + meetupId + payer/payee`，**不依赖约球域模型**。
- 约球域**零改动**（报名/审批/退出/状态机都不动）。
- 「发起收款」入口的鉴权（必须是发起人、活动必须已结束、应付人必须是参与者）由 **app 层编排**校验，调用约球域只读查询取参与者名单与发起人。

---

## 3. 聚合根与模型

本节给出职责概览，聚合根的充血行为签名与领域服务的抽象能力签名见 §15（方法级与状态机级）。

支付域 2 个聚合根 + 1 张回调留痕表：

### 3.1 PaymentOrder（支付单 / 收款流水）—— 聚合根
一个参与者的一笔应付。
- 标识 `bizId`（雪花，兼作渠道 `out_trade_no`，天然幂等）。
- 归属：`collectionBatchId`（同一次发起收款）+ `meetupId` + `payerUserId` + `payeeUserId`（发起人）。
- 金额（分）：`baseAmount` 本金 / `feeAmount` 手续费 / `payAmount` 实付。
- 渠道：`channel`、`channelTransactionId`、`prepayId`。
- 状态：`PENDING → PAID`，或 `PENDING → CLOSED`（超时），`PENDING → FAILED`。

```java
public class PaymentOrder {
    public static PaymentOrder create(PayChannelEnum channel, String batchId, String meetupId,
                                      String payerUserId, String payeeUserId,
                                      int baseAmount, BigDecimal feeRate, int timeoutMinutes) { ... }
    private int calcFee(int base, BigDecimal rate);   // ceil(base*rate)
    public void markPaid(String transactionId);       // 幂等：已 PAID 直接返回
    public void close();                              // 仅 PENDING 可关
    public void assertPaid();                         // 分账前置
}
```

### 3.2 SettlementOrder（分账单）—— 聚合根
收款成功后向发起人分账。**微信分账针对单笔交易**，故与支付单 1:1（`uk_payment_order` 唯一防重）。
- `shareAmount = baseAmount`（手续费覆盖微信通道费，平台不赚差价）。
- 状态：`PENDING → PROCESSING → FINISHED / FAILED`。

### 3.3 PaymentLog（支付全链路留痕）—— 非聚合根
支付全链路的关键动作落库，供对账与排查。按 `log_type` 区分类型，回调只是其中一类：
- `COLLECT`：发起人发起收款、为参与者建支付单。纯留痕，落库即 `PROCESSED`。
- `PREPAY`：参与人每一次前端发起支付（调起渠道下单）。纯留痕，落库即 `PROCESSED`。
- `CALLBACK`：渠道异步回调（支付/分账，原始报文解密后落库），落 `RECEIVED` 待处理，处理成功转 `PROCESSED`；超时仍 `RECEIVED` 由补偿扫描重放。
`process_status` 只对 `CALLBACK` 有推进语义，补偿扫描条件 `WHERE log_type=CALLBACK AND process_status=RECEIVED` 天然不会扫到 `COLLECT/PREPAY`。关联 `ref_id`（payment_order/payment_settlement 的 biz_id）串起全链路。

### 3.4 ShareReceiver（分账接收方账本）—— 聚合根
**微信分账必须先「添加接收方」(`receivers/add`) 才能分账，且接收方列表有数量上限（默认 2w）。** 微信无好用的"列出全部接收方"接口，故我方维护账本作为真相。
- 标识：`(channel, account=openid)`。归属 `userId`（发起人）。
- 状态：`bind_status` `BOUND/UNBOUND`；`last_share_time`（取发起收款时间）作 LRU 淘汰依据。
- 行为：`ensureBound(userId, openid)` 幂等绑定（未绑定则调渠道 add）、`touch()` 刷新 last_share_time、`unbind()` 淘汰（删除前须无 PROCESSING 分账）。

### 3.5 值对象 / 枚举（name 大写）
- `PayChannelEnum`：`WECHAT`（后续 `ALIPAY`）
- `BizTypeEnum`：`MEETUP_COLLECT`（MVP 仅此一值=活动后发起收款；后续退款/预收等再拓展）
- `PaymentStatusEnum`：`PENDING/PAID/CLOSED/FAILED`
- `SettlementStatusEnum`：`PENDING/PROCESSING/FINISHED/FAILED`
- `ShareReceiverStatusEnum`：`BOUND/UNBOUND`
- `PaymentLogTypeEnum`：`COLLECT/PREPAY/CALLBACK`（见 §3.3）
- `PaymentViewStatus`（对外视图态，详见 §5.5）：`NONE/UNPAID/PAID/CLOSED`
- `CollectionStateEnum`（发起人收款入口态，详见 §5.5）：`INITIABLE/ONGOING/ENDED`
- 金额一律 `int` 分；手续费 `ceil`。

MVP 不设独立「收款单」聚合根，用 `collection_batch_id` 给一次发起收款的多笔支付单分组；详情页收款进度由 `payment_order` 按 `meetup_id` 聚合查询得到。后续若需要收款单头（备注、关闭收款等）再升格为聚合根。

---

## 4. 支付渠道抽象（多渠道预留）

按项目约定：访问三方 → `Client` 后缀；操作中间件 → `Repository` 后缀。

### 4.1 domain 层接口 `com.rally.domain.payment.gateway`
```java
public interface PaymentChannelClient {
    PayChannelEnum channel();
    PrepayResult        prepay(PaymentOrder order, String payerOpenid);  // JSAPI 下单，返回小程序拉起参数
    ChannelTradeResult  queryTrade(String outTradeNo);                   // 查单（对账兜底）
    void                addShareReceiver(ShareReceiver receiver);        // 添加分账接收方（分账前置）
    void                deleteShareReceiver(ShareReceiver receiver);     // 删除接收方（LRU 淘汰）
    ChannelShareResult  profitShare(SettlementOrder order);             // 发起分账
    ChannelShareResult  queryProfitShare(String outOrderNo);            // 查分账结果
    CallbackResult      verifyAndParse(String body, Map<String,String> headers); // 验签+解密回调
}
public interface PaymentOrderRepository { ... }
public interface SettlementOrderRepository { ... }
public interface ShareReceiverRepository { ... }
public interface PaymentLogRepository { ... }
```

### 4.2 渠道路由（domain 层）
```java
@Component
public class PaymentChannelRouter {
    private final Map<PayChannelEnum, PaymentChannelClient> clients; // Spring 注入全部实现按 channel() 建 map
    public PaymentChannelClient route(PayChannelEnum channel); // 缺失抛 PAYMENT_CHANNEL_NOT_SUPPORTED
}
```

### 4.3 infrastructure 层实现 `com.rally.client.pay.wechat`
- `WechatPayClient implements PaymentChannelClient`，基于官方 SDK `wechatpay-java`（JSAPI 下单 / 分账 / 查单 / 回调验签解密 APIv3）。在 `rally-infrastructure/pom.xml` 引入依赖。
- `WechatPayProperties`（`@ConfigurationProperties("wechat.pay")`）：`mchId`、`appId`（复用 `wechat.mini.app-id`）、`apiV3Key`、`merchantSerialNumber`、`privateKeyPath`、`payNotifyUrl`、`shareReceiver`（发起人 openid 绑定）等。
- 后续接支付宝：新增 `AlipayClient implements PaymentChannelClient`，**支付域与 app 编排零改动**。

---

## 5. 关键流程

### 5.1 发起收款（活动结束后，发起人触发，MVP 仅一次）
```
POST 发起收款(meetupId, 勾选应付人列表, 总额)
app(CollectionAppService):
  1. 加载 meetup（只读）→ 校验 realStatus==FINISHED 且 当前用户==发起人
  2. 一次性校验：该 meetup 已存在任意 payment_order → 拒绝（COLLECTION_NOT_ALLOWED）。MVP 每场仅一次发起，不支持重发/补收
  3. 校验勾选应付人均为有效参与者（JOINED/REVIEWED/SKIPPED，排除发起人自己）
  4. ★分账接收方前置：ShareReceiverDomainService.ensureBound(发起人, 发起人openid)
     - 未绑定 → 调 addShareReceiver 添加并落 BOUND；已绑定 → touch 刷新 last_share_time
     - 提前在发起阶段暴露绑定失败，避免支付成功后分账卡住
  5. 计算每人 baseAmount（总额平摊 ceil，余数算入第一人）
  6. 生成 collectionBatchId；按应付人逐个 PaymentOrder.create(PENDING)
     - expire_time：配置了超时则 now+分钟数，否则 null（默认不超时，见 §5.4）
     - 幂等：uk_batch_payer
  7. 通知应付人「{发起人}发起了收款，请支付」（微信订阅消息，复用现有 NotifySubscribe）
返回：收款批次概览
```
支付参数获取：参与者点支付时调 `prepay` 接口，用其 openid JSAPI 下单返回 `paySign` 等拉起参数（openid 由现有登录态/`user` 取）。

### 5.2 支付与回调
```
参与者调起微信支付 → 微信异步回调 /api/rally/wechat/pay/notify
app(PaymentCallbackAppService):
  1. WechatPayClient.verifyAndParse 验签+解密 → 落 payment_log(log_type=CALLBACK)
  2. 按 out_trade_no 加载 PaymentOrder → markPaid(幂等，已 PAID 直接 200)
  3. 触发分账：SettlementAppService.share(paymentOrder)  // 可同步或转异步 Job
  4. 按微信要求返回成功应答
```

### 5.3 分账
```
SettlementAppService.share(paymentOrder):
  1. assertPaid + 幂等（uk_payment_order）→ 建 SettlementOrder(PENDING)
  2. 兜底确认接收方已 BOUND（正常发起收款时已绑定，此处防御）
  3. WechatPayClient.profitShare → PROCESSING（记 channel_order_id）
  4. 微信分账无可依赖的「分账成功回调」→ 以 SettlementReconcileJob 定时 queryProfitShare 为权威 → FINISHED/FAILED
```
分账与支付不同：支付有强回调（§5.2），分账的「请求分账」异步且无逐单完成回调，**最终结果以主动查询为准**。微信「分账动账通知」面向资金动账，仅作可选加速（§16 分账回调地址为可选），不依赖它推进状态。
三方约束：发起人需作为微信**个人分账接收方**预先绑定（发起收款时 ensureBound，见 §5.1 第 4 步；接收方账本与上限管理见 §5.7）；分账须在交易资金解冻窗口内完成（收款后即分账，满足）。

**渠道接口规格（V3 全套，APIv3 私钥签名 + APIv3 密钥 AEAD 解密）：**
- 添加接收方：`POST https://api.mch.weixin.qq.com/v3/profitsharing/receivers/add`。请求体：`appid` + `type=PERSONAL_OPENID` + `account`（openid）+ `relation_type=USER`（个人接收方默认）+ 可选 `name`（敏感字段，SDK 自动用平台证书 RSAES-OAEP 加密）。
- 删除接收方：`POST .../v3/profitsharing/receivers/delete`（同 type/account）。LRU 淘汰前置须确认无 PROCESSING 分账。
- 请求单次分账：`POST .../v3/profitsharing/orders`。请求体 `transaction_id`（被分账交易）+ `out_order_no`（= SettlementOrder.bizId）+ `receivers[]`（含 type/account/amount/description，受 V3 单字段加密规则）+ `unfreeze_unsplit=true` 单次分账即解冻剩余金额。
- 查询分账结果：`POST .../v3/profitsharing/orders/{out_order_no}/query`（最终态权威来源；状态枚举 PROCESSING/FINISHED）。
- 分账动账通知（可选加速）：商户平台配置 https 地址，AEAD_AES_256_GCM 解密，事件 `PROFITSHARING.*`。

### 5.4 超时机制（默认不超时）
后付费收款挂着不付不占名额、不影响他人，**默认不设业务超时**，何时停止收款由发起人手动决定（§5.6）。区分两层：
- **系统超时（业务层，默认关闭）**：`payment.pay_timeout_minutes` **默认 0 = 不超时**，`payment_order.expire_time = null`。仅当配置为正整数时，`expire_time = now + 分钟数`，由 `PaymentTimeoutJob` 到期关单。
- **渠道超时（微信层，固有）**：`prepay_id` 有效期约 2h，到期前端**重新请求下单**即可（订单本身不超时，可反复下单）。下单时若 `expire_time` 非空则传 `time_expire` 对齐，否则不传（用微信默认）。

### 5.4.1 对账与兜底（支付流水核心诉求）
- **流水**：`payment_order`（收款）+ `payment_settlement`（分账）+ `payment_log`（建单/下单/回调全链路原文）三层留痕。
- **PaymentTimeoutJob**（仅在配置了超时时生效）：扫 `status=PENDING && expire_time IS NOT NULL && expire_time<now` → `queryTrade` → 已支付补推 PAID + 触发分账；未支付则 `close()` + 微信 `closeOrder`。
- **SettlementReconcileJob**：扫 `status=PROCESSING` → `queryProfitShare` → 推进 FINISHED/FAILED；失败可重试。
- **回调漏处理补偿**：扫 `payment_log WHERE log_type=CALLBACK AND process_status=RECEIVED` 超时未处理的重放。
- Job 开关 `job.payment_timeout.enabled` / `job.settlement_reconcile.enabled`，cron 配 `application-prod.yml`（符合 job 规范）。

### 5.4.2 关闭收款（发起人手动）
```
POST 关闭收款(meetupId)
app(CollectionAppService.close):
  1. 校验当前用户==发起人（取约球只读）且该 meetup 存在收款（payment_order）
  2. PaymentDomainService.closeBatch：查本场全部 status=PENDING → 逐个 close() + 微信 closeOrder（渠道调用在 domain 层编排）
  3. 已 PAID 的不动（钱已收、已分账）
```
关闭后该场剩余订单只有 PAID/CLOSED 两类终态，收款生命周期结束（MVP 不可再发起）。详情页按钮态推导（无需批次头表）见 §5.6：无任何 payment_order → 「发起收款」；存在 PENDING → 「关闭收款」；有单但无 PENDING → 收款已结束。

### 5.5 参与人支付状态透出（详情页 / 待处理列表）

**原则：支付状态是支付域的读模型，不沉淀到 registration（不加字段、不扩 `RegistrationStatusEnum`）。** 支付状态唯一真相来源是 `payment_order.status`（回调驱动），由 **app 层在详情编排时组装到 `MeetupDetailDTO.payment`**，与 `recap`/`weather` 等子对象同样在 app 层填充，registration 实体与枚举零改动。

理由：registration 是「报名生命周期」、payment 是正交的「支付生命周期」，混入同一枚举/字段会导致二维状态爆炸、跨域回写耦合、回调乱序的一致性风险，并阻碍后续退款/多渠道扩展。`MeetupDetailDTO` 持有支付视图子对象属 meetup→payment 正向引用（payment 域不反向依赖 meetup，红线不破）。

做法（详情页）：`MeetupDetailDTO` 新增 `payment` 子对象（支付域 `MeetupPaymentViewDTO`），app 层详情编排时与其他信息一并组装。
```java
// 一次查本场全部支付单（idx_meetup_status），payerUserId -> 视图状态
Map<String, PaymentViewStatus> payMap = paymentQueryService.statusByMeetup(meetupId);
detailDTO.setPayment(PaymentAppConvertMapper.INSTANCE.toView(payMap, meetupId, userId)); // 与 recap/weather 同样在 app 层 set
```
- 关联键 `(meetup_id, payer_user_id)`，`payer_user_id == registration.user_id`；逐人支付态放 `payment.participantStatus`（userId -> 态），前端用 `participants[i].userId` 查询，**不 join**、不回写 `ParticipantDTO`。
- 列表页多活动用 `meetup_id IN (...)` 批量查，避免 N+1。
- 待处理列表「我有待支付」：查 `payment_order WHERE payer_user_id=我 AND status=PENDING`，作为待办项，同样不入 registration。

对外视图枚举 `PaymentViewStatus`（独立于内部 `PaymentStatusEnum`，避免内部态外泄）：

| 视图态 | 含义 | 来源 |
|---|---|---|
| `NONE` | 未发起收款 | 无 payment_order |
| `UNPAID` | 待支付 | payment_order=PENDING |
| `PAID` | 已支付 | payment_order=PAID |
| `CLOSED` | 已取消/超时 | payment_order=CLOSED/FAILED |

### 5.6 详情页支付交互（ActionStateEnum 不扩展）

支付入口与约球主操作是两条正交生命周期：活动结束(FINISHED)时，发起人可能**同时**看到「一键评价」（`actionState=FINISHED_JOINED`）与「发起收款」，二者并存、非互斥主操作，无法压进同一枚举。因此 `ActionStateEnum` 不新增任何支付态——否则会产生 `评价态 × 支付态 × 角色` 的二维爆炸，并破坏约球域零改动红线。

支付交互按角色拆成两个正交维度，统一收口到 `MeetupDetailDTO.payment`（支付域 `MeetupPaymentViewDTO`），由 app 层详情编排组装，registration 与 `ParticipantDTO` 零改动：

| 维度 | 字段 | 谁可见 | 取值与前端 |
|---|---|---|---|
| 发起人·收款入口 | `payment.collectionState` | 仅发起人且 realStatus==FINISHED，否则 null | `INITIABLE` 无单→「发起收款」/ `ONGOING` 有 PENDING→「关闭收款」/ `ENDED` 有单无 PENDING→「收款已结束」 |
| 参与人·我的支付 | `payment.myPaymentStatus` | 当前用户为应付人，否则 null | `UNPAID`→「去支付」/ `PAID`→已支付 / `CLOSED`→已取消 |
| 参与人列表·逐人 | `payment.participantStatus`（`Map<userId, PaymentViewStatus>`） | 发起人看列表收款进度 | 前端用 `detail.participants[i].userId` 查表，缺省 `NONE` |

- 「待支付」是参与人维度（当前用户支付单态），「何时可发起收款」是发起人维度（本场是否已有单），两字段分别表达、互不污染 actionState。
- `payment.collectionState` 由 `PaymentQueryDomainService.hasAnyOrder/hasPending` 推导；`payment.myPaymentStatus` 取当前用户本场 `payment_order` 的 `toViewStatus()`；`payment.participantStatus` 由 `statusByMeetup` 得到。
- `MeetupDetailDTO` 新增 `payment` 字段 `MeetupPaymentViewDTO { collectionState, myPaymentStatus, participantStatus }`，与 `recap`/`weather` 子对象同构，均由 app 层详情编排组装；`MeetupPaymentViewDTO` 与视图枚举定义在支付域。

### 5.7 分账接收方账本与上限管理

微信分账接收方是**有状态、有上限**的渠道资源，我方用 `payment_share_receiver` 账本管理其生命周期。

- **绑定（MVP）**：发起收款时 `ensureBound(发起人, openid)`——账本无 BOUND 记录则调 `addShareReceiver` 添加并落库；已存在则 `touch` 刷新 `last_share_time`（取发起收款时间）。幂等：`uk_channel_account` + 微信"已存在"视为成功。
- **openid 维度**：必须是本小程序 appid 下的发起人 openid（登录态已有 user→openid 映射）。
- **淘汰（后续，非 MVP）**：接收方接近 `payment.wechat.share_receiver_max`（默认 2w）时，`ShareReceiverEvictJob` 按 `(bind_status=BOUND, last_share_time 最久)` 选取，**确认其无 PROCESSING 分账单后**调 `deleteShareReceiver` 删除、置 `UNBOUND` 腾名额；该用户下次收款重新 `ensureBound`。早期名额充裕，先不做。
- **待验证坑**：V3 添加接收方接口个人接收方 `type=PERSONAL_OPENID`，必填 `relation_type`（个人收款人统一用 `USER`）；大额分账可能需 `name` 个人真实姓名校验（小额免），SDK 自动用平台证书加密 name 字段；接收方 add 后才能在 `createOrder` 中作为 `receivers[i].account` 使用。

---

## 6. 金额与手续费

- `baseAmount`：发起收款时按总额平摊或发起人直填每人金额（见 D1）。
- `feeAmount = ceil(baseAmount × feeRate)`，`feeRate` 取 `payment.wechat.fee_rate`（千 6）。
- `payAmount = baseAmount + feeAmount`，前端展示拆分（本金 + `payment.wechat.fee_desc`）。
- 分账 `shareAmount = baseAmount`。
- 示例：每人本金 50.00 元 → 手续费 `ceil(5000×0.006)=30` 分 → 实付 50.30 元 → 分账给发起人 50.00 元。

---

## 7. 表设计

完整见 `docs/sql/payment_tables.sql`：

| 表 | 聚合根 | 说明 |
|---|---|---|
| `payment_order` | PaymentOrder | 收款流水；`collection_batch_id` 分组；`uk_batch_payer` 防重复发起 |
| `payment_settlement` | SettlementOrder | 分账单，与支付单 1:1（`uk_payment_order`） |
| `payment_share_receiver` | ShareReceiver | 分账接收方账本（绑定状态 + LRU 淘汰依据） |
| `payment_log` | —（日志） | 全链路留痕（COLLECT 建单 / PREPAY 下单 / CALLBACK 回调），对账/排查 |

约定：金额「分」；`biz_id` 雪花兼作渠道 `out_*_no`；状态存大写枚举 name。

---

## 8. 代码落地结构（COLA 分层）

```
rally-domain/com/rally/domain/payment/
  ├─ model/    PaymentOrder, SettlementOrder, ShareReceiver, PaymentLog, PrepayResult, ChannelTradeResult, ChannelShareResult, CallbackResult, MeetupPaymentViewDTO
  ├─ enums/    PayChannelEnum, BizTypeEnum, PaymentStatusEnum, SettlementStatusEnum, ShareReceiverStatusEnum, PaymentLogTypeEnum, PaymentViewStatus, CollectionStateEnum
  ├─ gateway/  PaymentChannelClient, PaymentOrderRepository, SettlementOrderRepository, ShareReceiverRepository, PaymentLogRepository
  └─ service/  PaymentCollectionPolicy, PaymentDomainService, SettlementDomainService, ShareReceiverDomainService, PaymentQueryDomainService, PaymentChannelRouter

rally-app/com/rally/payment/
  ├─ CollectionAppService       发起收款 / 关闭收款（校验发起人/已结束/参与者 + 批量建单 + 通知）
  ├─ PaymentAppService          获取支付参数(prepay) + 处理支付回调
  ├─ SettlementAppService       分账编排
  └─ convert/ PaymentAppConvertMapper（MapStruct，出参 DTO / 入参 Cmd；含 payMap → MeetupPaymentViewDTO）

约球域 `MeetupDetailDTO` 新增 `payment` 字段（支付域 `MeetupPaymentViewDTO`），app 层详情编排时 set，registration 与 `ParticipantDTO` 零改动；`MeetupPaymentViewDTO` 与 `PaymentViewStatus`/`CollectionStateEnum` 定义在支付域，约球域持有它属 meetup→payment 正向引用（payment 域不反向依赖 meetup）。

rally-infrastructure/com/rally/
  ├─ client/pay/wechat/  WechatPayClient(implements PaymentChannelClient), WechatPayProperties
  └─ db/payment/         PaymentOrderPO/Mapper/Service/RepositoryImpl, SettlementOrder*, ShareReceiver*, PaymentLog*

rally-adapter/com/rally/
  ├─ web|app/payment/    CollectionController（发起收款/关闭收款）, PaymentController（取支付参数/查我的待付）
  ├─ wechat/pay/         WechatPayNotifyController（支付异步回调，渠道专属置 wechat 包）
  └─ job/                PaymentTimeoutJob, SettlementReconcileJob
```
回调 Controller 不捕获异常；验签解密在 `WechatPayClient` 内。对外对象 DTO 结尾、入参 Cmd 结尾、`@RequestParam("key")`、`spring-boot-starter-validation` 校验。

---

## 9. 配置项（默认值在 `SystemConfigKey` 枚举维护，不落 `sys_config` 表）

| key | 默认 | 说明 |
|---|---|---|
| `payment.wechat.fee_rate` | 0.006 | 手续费率千 6 |
| `payment.wechat.fee_desc` | 含微信支付手续费 0.6% | 展示文案 |
| `payment.pay_timeout_minutes` | 0 | 0=不超时（默认）；>0 才主动关单 |
| `payment.wechat.share_receiver_max` | 20000 | 分账接收方上限，接近时 LRU 淘汰（后续） |

`application-prod.yml` 追加 `wechat.pay.*` 与 `job.settlement_reconcile.enabled` + cron；`job.payment_timeout.enabled` 默认可不开（仅在配置了超时时启用）。

---

## 10. 异常码（`BizErrorCode` 追加）

`PAYMENT_ORDER_NOT_FOUND`、`PAYMENT_ALREADY_PAID`、`PAYMENT_CHANNEL_NOT_SUPPORTED`、`PAYMENT_CREATE_FAILED`、`COLLECTION_NOT_ALLOWED`（非发起人/活动未结束/已发起）、`SETTLEMENT_FAILED`。统一 `Assert.xxx(cond, BizErrorCode.XXX)`。

---

## 11. 幂等、一致性与安全

- **幂等**：`out_trade_no/out_order_no = biz_id` 唯一；回调按状态机短路（已终态直接返回成功）；`uk_batch_payer`/`uk_payment_order` 防重复发起与重复分账；状态推进用条件更新（`where status=期望值`）。
- **权威以服务端订单状态为准**，不信前端回跳；查单 + 对账 Job 兜底渠道漏推。
- **金额后端计算**，前端不可传金额。
- **事务边界**：app 层 `@Transactional` 编排「订单状态推进 + 分账建单」。

---

## 12. 后续演进（接口已预留，非 MVP）

1. **退款**：新增 `RefundOrder` 聚合根 + `payment_refund` 表 + `PaymentChannelClient.refund/queryRefund`（误收、纠纷）。
2. **重发/补收**：MVP 每场仅一次发起、不可重发；后续支持对未付者重新催收或补建订单。
   **分账接收方淘汰**：接近上限时 `ShareReceiverEvictJob` 按 LRU 删除最久未用接收方腾名额（见 §5.7），早期名额充裕暂不做。
3. **报名预收 / 多退少补**：报名即缴费 + 待支付状态 + 活动后按实到结算补退（前述重方案，按需再上）。
4. **多渠道**：新增 `AlipayClient` 等实现。
5. **收款单头**：升格 `CollectionOrder` 聚合根（收款备注、收款进度统计、多次收款）。
6. **线上/线下模式**：`rally_meetup.pay_mode` 区分微信收款 / 线下自理。

---

## 13. 已确认决策点

- **D1 每人金额口径** ✅ 发起人输入**总额**，系统按应付人数平摊（`ceil`，余数算入第一人）。
- **D2 应付人范围** ✅ 允许发起人手动勾选部分参与者收款（默认全体有效参与者，排除发起人）。
- **D3 手续费** ✅ 收取千 6：实付 = 本金 + `ceil(本金×0.006)`，前端展示拆分。
- **D4 分账时机** ✅ 每笔支付成功即分账。
- **D5 支付状态透出** ✅ 不入 registration，由 app 层关联 `payment_order` 组装 `PaymentViewStatus`（见 §5.5）。
- **D6 超时** ✅ 默认不超时（`pay_timeout_minutes=0`，`expire_time=null`）；配置正整数才主动关单（见 §5.4）。
- **D7 一次发起 + 手动关闭** ✅ MVP 每场仅一次发起收款；发起人可手动关闭收款（未付单 → CLOSED，已付不动，见 §5.4.2）。
- **D8 分账接收方账本** ✅ 我方维护 `payment_share_receiver`，发起收款时 `ensureBound` 提前绑定，`last_share_time` 取发起收款时间；上限 LRU 淘汰列入后续（见 §5.7）。分账走 V3 `/v3/profitsharing/orders` 全套（添加接收方 / 发起 / 查询），接收方 `type=PERSONAL_OPENID` + `relation_type=USER`。待验证：大额姓名校验。
- **D9 详情页支付交互** ✅ `ActionStateEnum` 不扩展支付态（避免 评价×支付×角色 二维爆炸、守 registration 零改动）；支付交互按角色拆为发起人维度 `collectionState` 与参与人维度 `myPaymentStatus` 两个正交字段，收口到 `MeetupDetailDTO.payment`（支付域 `MeetupPaymentViewDTO`），app 层详情编排组装（见 §5.6）。

---

## 14. 注意事项与实施须知（交接 checklist）

本节供后续实现会话直接照做。**本文档 + `docs/sql/payment_tables.sql` 即唯一真相**，实现时无需依赖任何历史对话。

### 14.1 架构与解耦红线（不可破坏）
- **registration 零改动**：不改 `RegistrationStatusEnum`、不在 registration 加支付字段、不回写 `ParticipantDTO`。支付状态由 app 层组装到 `MeetupDetailDTO.payment`（§5.5/§5.6）。
- **支付域不依赖约球域**：支付域只认 `meetupId + payer/payee + bizType`；取参与者名单/发起人由 app 层调约球域**只读查询**完成。`MeetupDetailDTO` 持有支付视图子对象属 meetup→payment 正向引用，方向合法。
- **领域对象获取走领域 service，不直接调 gateway**（项目规范）。

### 14.2 资金与幂等（每一项都必须落实）
- 金额单位**分**、`int`；手续费 `feeAmount = ceil(base × rate)`；金额**一律后端计算**，前端不可传。
- `biz_id` = `IdWorker.getIdStr()`，同时作渠道 `out_trade_no/out_order_no`，天然幂等。
- 回调/状态推进**幂等**：已终态直接返回成功；状态推进用**条件更新**（`where status=期望值`）防并发覆盖。
- 唯一键防重：`uk_batch_payer`（防重复发起）、`uk_payment_order`（分账 1:1）、`uk_channel_account`（接收方）。
- **以服务端订单状态为权威**，绝不信前端回跳；查单 + 对账 Job 兜底渠道漏推。

### 14.3 微信支付联调要点（坑）
- **prepay 在付款人点支付时才调**（非发起收款时）；`prepay_id` ~2h 过期，订单仍 PENDING 则重新下单；配置了超时才传 `time_expire` 对齐。
- **分账针对单笔交易**（依赖 `transaction_id`），故分账单与支付单 1:1；分账须在资金**解冻窗口内**完成（收款后即分账，满足）。
- **分账接收方必须先 `add` 才能分**、列表有上限（默认 2w）；发起收款时 `ensureBound` 提前绑定；删除（淘汰）前须确认该接收方**无 PROCESSING 分账单**。
- 接收方 openid 必须是**本小程序 appid 下**的 openid。
- 回调：`verifyAndParse` 必须**验签 + APIv3 解密**，原文落 `payment_log(log_type=CALLBACK)`，并按微信要求返回成功应答格式。
- **待联调前确认的三方资质**（外部依赖，阻塞联调，非代码）：商户号、APIv3 密钥、商户 API 私钥 `apiclient_key.pem`、商户证书序列号、**分账权限开通**、大额是否需 `name` 姓名校验。

### 14.4 项目规范对齐（编码前必读 CLAUDE.md / rules）
- 新增配置键的**默认值统一在 `SystemConfigKey` 枚举维护，不落 `sys_config` 表**；仅当需覆盖默认值时才在 `sys_config` 落对应 `config_key`。
- 异常用 `Assert.xxx(cond, BizErrorCode.XXX)`；需在 `BizErrorCode` 补 §10 所列异常码。枚举 name 全大写。
- DB 走 `Repository(domain) → RepositoryImpl(infra) → Service(MP) → Mapper`；PO 用 `@TableName`；优先 `LambdaQueryWrapper`。
- 对象转换用 **MapStruct 单例**（`INSTANCE` 直调）；对外对象 `DTO` 结尾、入参 `Cmd` 结尾。
- controller 分渠道包：通用接口 `web/`、渠道专属（微信回调）`wechat/`；不捕获异常；`@RequestParam("key")`；用 `spring-boot-starter-validation` 校验。
- Job 放 `com.rally.job`，开关 `job.[key].enabled`（prod=true），cron 在 `application-prod.yml`。
- `rally-infrastructure/pom.xml` 引入 `wechatpay-java` SDK。
- 新增 api 完成后提供 curl。

### 14.5 建议落地顺序（分阶段，避免一次性大改）
- **P0（本地可验证，不依赖微信联调）**：支付域骨架（4 表 PO/Mapper/Repository + 聚合根 + 枚举）+ 发起收款/关闭收款 app 编排 + 接口 + 详情页支付状态透出；`WechatPayClient` 先留**桩实现**。
- **P1（联调）**：`WechatPayClient` 接 SDK（下单/回调/查单）+ 支付回调 Controller。
- **P2（联调）**：分账接收方 `ensureBound` + 按笔分账 + `SettlementReconcileJob`。
- **P3**：对账兜底完善（超时关单 Job 视配置启用、回调补偿）。
- 退款 / 多退少补 / 多渠道 / 接收方 LRU 淘汰：见 §12，均非 MVP。

---

## 15. 领域能力细化（聚合根充血模型 + DomainService 抽象）

把 §5 的流程从 app 编排下沉到领域层。职责红线对齐 `Meetup`/`RegistrationDomainService` 现有写法：

- **聚合根 = 状态 + 不变量 + 状态机**：所有校验用 `Assert.xxx(cond, BizErrorCode.XXX)` 或抛 `BusinessException`，状态流转只发生在聚合根内（如 `markPaid`），聚合根不碰 Repository / Client / 事务。
- **Policy = 无副作用的领域计算与发起校验**：`@Service`，承载平摊算法与发起收款的参数/参与者校验，对齐 `MeetupPolicy`。
- **DomainService = 薄编排**：`@Service @RequiredArgsConstructor`，职责 = 取聚合根（`get`）→ 调聚合根行为 / Policy → `Repository` 持久化 / `Client` 访问三方。状态推进落库一律条件更新（`where status=期望值`）防并发覆盖。
- **app 层只做跨域编排与鉴权**：取约球域只读名单、`UserContext.get()`、`@Transactional`，不写支付业务规则。

### 15.1 PaymentOrder（支付单）—— 充血模型

状态机：`PENDING ──markPaid──▶ PAID`；`PENDING ──close──▶ CLOSED`；`PENDING ──markFailed──▶ FAILED`。终态：`PAID/CLOSED/FAILED`。

```java
@Getter
public class PaymentOrder {
    private PaymentOrderData data; // bizId / collectionBatchId / meetupId / payer / payee / 金额 / channel / status / expireTime ...

    /** 工厂：算手续费 + 生成 bizId(out_trade_no) + 置 PENDING + 算 expireTime（timeoutMinutes<=0 则 null）。金额一律后端算。 */
    public static PaymentOrder create(PayChannelEnum channel, String batchId, String meetupId, String payerUserId, String payeeUserId, int baseAmount, BigDecimal feeRate, int timeoutMinutes) { ... }

    private static int calcFee(int base, BigDecimal rate); // ceil(base*rate)

    // —— 状态机（幂等 + 条件流转，校验在内）——
    public void markPaid(String transactionId);  // 已 PAID 直接返回（幂等）；非 PENDING 抛 PAYMENT_ALREADY_PAID/状态非法；置 PAID + 记 channelTransactionId
    public void close();                          // 已 CLOSED 直接返回；仅 PENDING 可关，否则抛（已 PAID 不可关）
    public void markFailed(String reason);        // 仅 PENDING → FAILED

    // —— 断言 / 判定 ——
    public void assertPaid();                     // 分账前置：非 PAID 抛 PAYMENT_ALREADY_PAID 的反向（状态非法）
    public void assertPayer(String userId);       // prepay 前置：当前用户必须是 payer，否则 BusinessException
    public boolean isPending();
    public boolean isExpired();                   // expireTime != null && expireTime < now（默认 null=永不超时）

    /** 内部态 → 对外视图态（避免内部枚举外泄，见 §5.5） */
    public PaymentViewStatus toViewStatus();      // PENDING→UNPAID / PAID→PAID / CLOSED|FAILED→CLOSED
}
```

### 15.2 SettlementOrder（分账单）—— 充血模型

状态机：`PENDING ──markProcessing──▶ PROCESSING ──markFinished──▶ FINISHED`；`PROCESSING ──markFailed──▶ FAILED`（可重试）。与支付单 1:1。

```java
@Getter
public class SettlementOrder {
    private SettlementOrderData data; // bizId(out_order_no) / paymentOrderId / payeeUserId / shareAmount / channelOrderId / status

    /** 工厂：从已支付的支付单建分账单。内部 order.assertPaid()；shareAmount = baseAmount（手续费不分账，见 §6）；置 PENDING。 */
    public static SettlementOrder createFrom(PaymentOrder order);

    public void markProcessing(String channelOrderId); // PENDING → PROCESSING，记渠道单号
    public void markFinished();                        // 已 FINISHED 幂等返回；PROCESSING → FINISHED
    public void markFailed(String reason);             // PROCESSING → FAILED
    public boolean isProcessing();
    public boolean canRetry();                         // FAILED 可重试
}
```

### 15.3 ShareReceiver（分账接收方账本）—— 充血模型

状态机：`UNBOUND ⇄ BOUND`。聚合根只管状态翻转，**调渠道 add/delete 由 DomainService 编排**（聚合根不碰 Client）。

```java
@Getter
public class ShareReceiver {
    private ShareReceiverData data; // channel / account(openid) / userId / bindStatus / lastShareTime

    public static ShareReceiver create(PayChannelEnum channel, String userId, String openid); // 初态 UNBOUND
    public boolean isBound();
    public void bind();                  // UNBOUND → BOUND + 刷新 lastShareTime（渠道 add 成功后由 service 调）
    public void touch(LocalDateTime t);  // 已绑定时刷新 lastShareTime（LRU 依据，取发起收款时间）
    public void unbind();                // BOUND → UNBOUND（前置“无 PROCESSING 分账”由 service 校验后调）
    public void assertBound();           // 分账前置防御
}
```

### 15.4 PaymentLog（全链路留痕）—— 非聚合根

```java
public class PaymentLog {
    // 发起人发起收款建单留痕，落库即 PROCESSED（纯留痕）
    public static PaymentLog collect(PayChannelEnum channel, String refId, String rawBody);
    // 参与人每次前端发起支付（下单）留痕，落库即 PROCESSED（纯留痕）
    public static PaymentLog prepay(PayChannelEnum channel, String refId, String rawBody);
    // 渠道回调留痕（支付/分账），落 RECEIVED 待处理
    public static PaymentLog callback(PayChannelEnum channel, String refType, String refId, String rawBody);
    public void markProcessed();  // RECEIVED → PROCESSED（仅 CALLBACK 有推进语义）
    public void markFailed(String reason);
}
```

### 15.5 DomainService 抽象领域能力

#### PaymentCollectionPolicy —— 发起收款的领域计算与校验（无副作用）
```java
@Service
public class PaymentCollectionPolicy {
    /** 总额平摊：ceil 平均，余数算入第一人（D1）。纯计算。 */
    int[] amortize(int totalAmount, int payerCount);
    /** 发起收款参数校验：总额>0、应付人非空、金额可平摊。校验失败抛 BizErrorCode.PARAM_ERROR。 */
    void assertCollect(int totalAmount, List<String> payerUserIds);
}
```

#### PaymentDomainService —— 收款单生命周期
```java
@Service @RequiredArgsConstructor
public class PaymentDomainService {
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentChannelRouter channelRouter;
    private final PaymentCollectionPolicy collectionPolicy;

    /** 发起收款：collectionPolicy.assertCollect + amortize → 逐个 PaymentOrder.create(PENDING) → 批量落库（uk_batch_payer 防重）。应付人名单已在 app 层取约球只读名单后传入。 */
    List<PaymentOrder> createBatch(String meetupId, String payeeUserId, List<String> payerUserIds, int totalAmount, PayChannelEnum channel);

    /** 取拉起参数：load → assertPayer → channelRouter.route(channel).prepay(order, openid)。 */
    PrepayResult prepay(String outTradeNo, String payerUserId, String payerOpenid);

    /** 回调/查单推进：load → markPaid(条件更新落库)。返回推进后的单用于触发分账。幂等。 */
    PaymentOrder markPaid(String outTradeNo, String transactionId);

    /** 关闭收款：查本场全部 PENDING → 逐个 close() + 渠道 closeOrder（best-effort）；已 PAID 不动（§5.4.2）。 */
    void closeBatch(String meetupId, String operatorUserId);

    /** 超时关单（PaymentTimeoutJob 用）：queryTrade 已付则补 markPaid，否则 close()+closeOrder。 */
    PaymentOrder timeoutCheck(PaymentOrder order);
}
```

#### SettlementDomainService —— 分账生命周期
```java
/** 收款成功后分账（可同步或转 Job）：assertPaid + 幂等(uk_payment_order 已存在直接返回) → ensureBound 防御 → SettlementOrder.createFrom → channel.profitShare → markProcessing 落库。 */
SettlementOrder share(PaymentOrder paidOrder);
/** 对账推进（SettlementReconcileJob 用）：channel.queryProfitShare → markFinished/markFailed 落库。 */
void reconcile(SettlementOrder order);
```

#### ShareReceiverDomainService —— 接收方账本
```java
/** 幂等绑定（发起收款前置，§5.1-4）：账本无 BOUND → channel.addShareReceiver + receiver.bind()；已绑定 → touch。微信“已存在”视为成功。 */
ShareReceiver ensureBound(String userId, String openid, PayChannelEnum channel);
/** 淘汰（非 MVP，§5.7）：校验无 PROCESSING 分账 → channel.deleteShareReceiver + unbind()。 */
void evict(ShareReceiver receiver);
```

#### PaymentQueryDomainService —— 支付读模型（§5.5，约球域零改动）
```java
Map<String, PaymentViewStatus> statusByMeetup(String meetupId);          // payerUserId -> 视图态，详情页合并
Map<String, PaymentViewStatus> statusByMeetups(List<String> meetupIds);  // 列表页 IN 批量，避免 N+1
List<PaymentOrder> myPending(String userId);                             // 待处理列表“我有待支付”
boolean hasAnyOrder(String meetupId);   // 详情页按钮态：无单→「发起收款」
boolean hasPending(String meetupId);    // 有 PENDING→「关闭收款」；有单无 PENDING→收款已结束
```

#### PaymentChannelRouter —— 渠道路由（见 §4.2）
`route(PayChannelEnum)`，缺失抛 `PAYMENT_CHANNEL_NOT_SUPPORTED`。

### 15.6 流程 → 领域能力映射（§5 落到方法）

| 流程（§5） | app 编排（跨域/鉴权/事务） | 领域能力（聚合根行为 ← DomainService） |
|---|---|---|
| 发起收款 §5.1 | 取约球只读名单 + 校验发起人/已结束/参与者 → `@Transactional` | `ShareReceiverDomainService.ensureBound` → `PaymentDomainService.createBatch`（`amortize` + `PaymentOrder.create`） |
| 取支付参数 §5.1 注 | `UserContext.get()`→openid | `PaymentDomainService.prepay`（`assertPayer` + `channel.prepay`） |
| 支付回调 §5.2 | 落 `payment_log(CALLBACK)` + 返回微信应答 | `PaymentDomainService.markPaid`（`PaymentOrder.markPaid` 幂等）→ `SettlementDomainService.share` |
| 分账 §5.3 | （回调内触发或转 Job） | `SettlementDomainService.share`（`assertPaid`+`createFrom`+`markProcessing`） |
| 关闭收款 §5.4.2 | 校验发起人 → `@Transactional` | `PaymentDomainService.closeBatch`（逐个 `PaymentOrder.close`） |
| 超时兜底 §5.4.1 | `PaymentTimeoutJob` | `PaymentDomainService.timeoutCheck` |
| 分账对账 §5.4.1 | `SettlementReconcileJob` | `SettlementDomainService.reconcile`（`markFinished/markFailed`） |
| 状态透出 §5.5/§5.6 | app 详情编排 set `MeetupDetailDTO.payment` | `PaymentQueryDomainService.statusByMeetup` / `hasAnyOrder` / `hasPending`（`PaymentOrder.toViewStatus`） |

落地结构对应 §8 的 `service/` 包：`PaymentCollectionPolicy`、`PaymentDomainService`、`SettlementDomainService`、`ShareReceiverDomainService`、`PaymentQueryDomainService`、`PaymentChannelRouter`；聚合根创建走 `PaymentOrder.create` 静态工厂，对齐 `MeetupFactory` 写法。

---

## 16. 待人工提供的资质与配置（联调前填写）

以下为外部资质与密钥，需人工从微信商户平台/小程序后台获取。**全 V3 方案**：JSAPI 下单 / 关单 / 查单 / 回调 / 添加接收方 / 删除接收方 / 发起分账 / 查询分账 全部走 APIv3，统一用 `apiV3Key + merchantSerialNumber + apiclient_key.pem`，不再需要 V2 双向证书 / V2 API 密钥 / p12。

配置默认值（`fee_rate` 等 4 项）统一在 `SystemConfigKey` 枚举维护、不落 `sys_config`。**密钥与回调地址不落 yml 明文，统一通过环境变量注入到 `.env`**（`application.yml` 用 `${VAR}` 引用），证书/私钥文件不入库、不进 git，建议落 `./cert/`（已在 `.gitignore`）。

| # | 项 | 配置 key / 环境变量 | 从哪获取 | 值                                          |
|---|---|---|---|--------------------------------------------|
| 1 | 商户号 | `wechat.pay.mch-id` / `WECHAT_PAY_MCH_ID` | 微信支付商户平台 → 账户中心 | 1747482285                                 |
| 2 | 小程序 AppID | `wechat.pay.app-id`（复用 `wechat.mini.app-id` / `WECHAT_APP_ID`） | 小程序后台 | wxa597b7c249b8ff81                         |
| 3 | APIv3 密钥（32 位） | `wechat.pay.api-v3-key` / `WECHAT_PAY_API_V3_KEY` | 商户平台 → API 安全 → 设置 APIv3 密钥（**唯一密钥，下单签名 + 回调 AEAD 解密 + 分账签名**） | bK9mQ2vXpL7nR4wTzC8aF3hJ6dY5sN1e           |
| 4 | 商户证书序列号 | `wechat.pay.merchant-serial-number` / `WECHAT_PAY_MERCHANT_SERIAL_NUMBER` | 商户平台 → API 安全 查看，或 `openssl x509 -in apiclient_cert.pem -noout -serial` | 2EF8A78B1AF4C276327D6D315706C6C2DCF0768C   |
| 5 | 商户 API 私钥（apiclient_key.pem） | `wechat.pay.private-key-path` / `WECHAT_PAY_PRIVATE_KEY_PATH` | 申请 API 证书时下载，**V3 签名唯一私钥** | ./cert/apiclient_key.pem                   |
| 6 | 支付回调地址 | `wechat.pay.pay-notify-url` / `WECHAT_PAY_NOTIFY_URL` | 我方域名 + `/api/rally/wechat/pay/notify`（HTTPS 公网可达） | https://api.fantasticmonkey.top/api/rally/wechat/pay/notify |
| 7 | 分账回调地址（可选） | `wechat.pay.share-notify-url` / `WECHAT_PAY_SHARE_NOTIFY_URL` | 我方域名 + `/api/rally/wechat/pay/share-notify`；不配则纯靠 `SettlementReconcileJob` 主动查询 | https://api.fantasticmonkey.top/api/rally/wechat/pay/share-notify |
| 8 | 分账接收方上限 | `payment.wechat.share_receiver_max`（`SystemConfigKey` 枚举默认 20000） | 微信文档 | 20000                                      |
| 9 | 发起人个人分账接收方 openid | 运行时由 `AccountRepository.findIdentifierByUser(userId, WECHAT_MINIAPP)` 反查，无需静态填 | 登录态映射 | （动态）                                       |

外部需开通/确认（无配置项，属商户资质动作）：
- 商户号已**开通分账权限**（未开通则 `addReceiver`/`createOrder` 直接 `NO_AUTH` 失败）。
- 大额分账是否需**个人真实姓名加密校验**（小额一般免，按商户等级与金额实测；接收方 `name` 字段，SDK 自动用商户证书加密）。
- 商户证书 / APIv3 密钥在有效期内；回调域名已在商户平台**配置并通过校验**。
- 平台证书由 SDK `RSAAutoCertificateConfig` 自动下载缓存，无需手动维护。
