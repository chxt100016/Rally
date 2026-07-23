# 模块 5：支付与晋级（席位管理）

## 职责
资格赛获胜后引导支付锁定正赛席位，处理支付回调推进状态，管理席位计数（满员即停资格赛），以及正赛退出的退款流程。是资格赛 → 正赛的枢纽。

## 聚合根 / 领域对象
- **TournamentEntry**：支付驱动其 status PAYING → WAITING、stage QUALIFY → MAIN，写 paidTime。
- **Tournament**：currentFilledSlots 的增减在此发生，满员（=totalSlots）触发资格赛停止。
- 复用 **Payment 域**（PaymentDomainService、WechatPayClient、PaymentStatusEnum）。

## 领域 Service 能力（TournamentPaymentService，编排 Payment 域）
- `createEntryOrder(entry)`：校验 entry 处于 PAYING、赛事未满员；按 entryFee + 手续费（复用 `SystemConfigKey.PAYMENT_WECHAT_FEE_RATE` 千6，承担方沿用 MEETUP_COLLECT 处理方式）下单，返回 `PrepayResult`（对齐 wx.requestPayment 入参）。
- `handlePayCallback(支付单)`：复用现有回调链路。成功后：entry PAYING→WAITING、stage→MAIN、写 paidTime、currentFilledSlots+1。校验满员边界（并发下用行锁/乐观更新防超卖）。
- `queryAndAdvance(entry)`：兜底主动查单（复用 `WechatPayClient.queryTrade`），据结果推进，避免卡在"确认中"。
- `refundForWithdraw(entry)`：正赛退出退款。**需新增能力**：现有支付体系无退款接口，需封装微信退款 API + 领域退款流程；退款成功后 currentFilledSlots-1 并放行模块 2 置 WITHDRAWN。（MVP 需评估是否纳入首版）
- 晋级承接：模块 4 判定资格赛胜者 → 置 entry status=PAYING（弹支付引导）；正赛胜者 → 进下一轮 WAITING（回模块 3 匹配）。

## 席位与资格规则
- 支付无超时，随时可下单。
- 支付成功锁席位；未支付资格保留但不占席位，他人继续资格赛。
- currentFilledSlots = totalSlots → 资格赛停止，未支付者失去资格。
- 到 qualifierEndTime 仍未满：设了则届时停，未设则永久开放至满员。

## 接口清单

### `POST /tournament/entry/pay`
下单。入参含 tournamentId。校验 entry 为 PAYING 且赛事未满 → 创建订单调 PaymentDomainService → 返回 `PrepayResult`（prepayId/timeStamp/nonceStr/packageVal/signType/paySign）。前端拿去调 wx.requestPayment。

### 支付回调（复用现有链路，非新增 HTTP）
微信异步回调 → 识别为赛事报名单 → 调 `handlePayCallback` 推进。前端 success 回调仅作刷新时机，真值以回调/查单为准；仍 PAYING 时前端展示"支付确认中"过渡。

### 退款（随 `entry/withdraw` 触发，见模块 2）
正赛退出时由模块 2 编排调 `refundForWithdraw`。

## 与其他模块的边界
- 席位计数是本模块唯一写入方，模块 1 只读展示。
- 退款是模块 2 withdraw 的正赛分支所依赖的能力。
- 状态推进以支付域真值为准，不新增"支付确认中"中间态（用 PaymentStatusEnum.PENDING 表达）。
