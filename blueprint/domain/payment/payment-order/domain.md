---
id: "@payment.payment-order"
kind: aggregate
tables:
  - name: payment_order
    columns: [id, biz_id, channel, biz_type, ref_biz_id, payer_user_id, base_amount, fee_amount, pay_amount, status, channel_transaction_id, prepay_id, prepay_expire_time, active_ref_key, description, pay_time, expire_time, create_time, update_time]
---

## 概要

守护一笔业务支付的金额、活跃唯一性与渠道确认状态。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 支付单 | 支付业务编号 `biz_id` | 建单时由雪花编号生成，同时作为渠道商户单号 | 渠道、业务引用、付款人、金额、预支付资料、状态和渠道结果 | `payment_order` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 应付金额 | 基础金额、手续费、实付金额 | `base_amount`、`fee_amount`、`pay_amount` |
| 业务身份 | 业务类型、关联业务编号、付款人、活跃关联键 | `biz_type`、`ref_biz_id`、`payer_user_id`、`active_ref_key` |
| 预支付凭证 | 渠道预支付编号与有效期 | `prepay_id`、`prepay_expire_time` |
| 支付结果 | 渠道流水、渠道成交时间 | `channel_transaction_id`、`pay_time` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 付款人引用 | `payer_user_id` | `@identity.user` | 只保存用户编号 |
| 业务对象引用 | `ref_biz_id` | 当前为 `@tournament.entry` | 由 `biz_type` 解释，支付单不推进报名状态 |
| 渠道交易引用 | `biz_id`、`channel_transaction_id` | 微信等外部支付渠道 | 聚合记录结论，不直接调用渠道 |

## 边界

一次加载与保存的单位是一张支付单。金额、付款人与业务身份建立后不可变；预支付资料和支付状态通过支付单命令修改。

赛事席位/报名推进、渠道预下单、查单、关单、回调日志和退款都在边界外。调用方把渠道结论传给聚合，并仅在聚合返回首次支付后推进关联业务。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `PENDING` | 活跃待支付，可保存或复用预支付凭证 | `PAID`、`CLOSED`、`FAILED` | `C3`、`C4`、`C5` |
| `PAID` | 渠道已确认付款，业务活跃键继续占用以阻止重复收费 | 无 | 无 |
| `CLOSED` | 未付款订单已关闭，释放活跃键，可重新建单 | 无 | 无 |
| `FAILED` | 建单流程确定失败，释放活跃键，可重新建单 | 无 | 无 |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 支付编号非空唯一，渠道、业务类型、业务引用和付款人非空且建立后不可变 | 支付根、业务身份、外部引用 | 商户单号和业务归属若可修改，渠道回执可能确认到另一报名或付款人 | `PAYMENT_IDENTITY_INVALID` |
| I2 | 基础金额为正，手续费等于 `ceil(baseAmount*feeRate)` 且非负，实付金额等于两者之和；三项建立后不可变 | 应付金额 | 渠道下单金额与本地业务金额必须一次确定，分开修改会造成实际收款与报名费不一致 | `PAYMENT_AMOUNT_INVALID` |
| I3 | `PENDING/PAID` 的活跃键必须等于 `bizType:refBizId:payerUserId` 且全表唯一；`CLOSED/FAILED` 必须为空 | 支付根、业务身份、状态 | 活跃唯一键与状态分开更新会短暂允许重复收费或永久阻止合法重建 | `PAYMENT_ACTIVE_CONFLICT` |
| I4 | `PAID` 必须同时具有非空渠道流水和渠道成交时间；非 PAID 不得伪造已支付结论 | 支付根、支付结果 | 状态、流水和时间共同证明付款事实，必须在一次条件更新中提交 | `PAYMENT_RESULT_INVALID` |
| I5 | 所有离开 `PENDING` 的迁移必须以 `biz_id+status=PENDING` CAS 并检查影响行数，终态不可覆盖 | 支付根、状态 | 回调、同步和超时任务可并发；只读后写会让关闭覆盖付款或多方都认为自己首次确认 | `PAYMENT_STATE_CONFLICT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 建立支付单 | 相同活跃业务键不存在 | 渠道、业务类型、业务引用、付款人、基础金额、费率、描述、可选超时时间 | `PENDING`，生成编号和活跃键 | 违反 I1/I2；活跃键已存在；超时时间不晚于建单时间 |
| C2 | 保存预支付凭证 | `PENDING` | prepayId、凭证有效期 | `PENDING` | prepayId 为空；有效期不晚于当前时间；并发状态已变化 |
| C3 | 确认渠道已付 | `PENDING` 或 `PAID` | 渠道流水、渠道成交时间 | CAS 成功返回 `FIRST_PAID` 并为 `PAID`；原已 PAID 返回 `ALREADY_PAID` | `CLOSED/FAILED`；渠道结果违反 I4；CAS 失败后重载不是 PAID |
| C4 | 关闭待付单 | `PENDING` | 当前时间、到期/主动关闭结论 | `CLOSED` 并清活跃键；仅 CAS 成功返回 `CLOSED_NOW` | 尚未到期且无主动关闭许可；并发已 PAID 或其他终态 |
| C5 | 标记建单失败 | `PENDING` | 失败摘要 | `FAILED` 并清活跃键 | 摘要为空；并发状态变化 |

预支付复用查询仅在 `PENDING`、`prepay_id` 非空且当前时间严格早于 `prepay_expire_time` 时返回可复用；取得凭证不表示已经付款。

## 边界情况

- 重复支付入口：活跃键已有 PENDING 时复用订单，已有 PAID 时返回已付款结论，不新建。
- PENDING 已超时：先由 C4 成功关闭，之后才允许建立新单；渠道关单只能在 `CLOSED_NOW` 后调用。
- 并发回调与主动同步：只有一个 C3 CAS 成功并返回 `FIRST_PAID`，另一方重载 PAID 后返回 `ALREADY_PAID`，不得重复推进报名。
- 并发支付确认与超时关闭：以数据库首个成功 CAS 为准；C4 未成功时禁止调用渠道关单。
- CLOSED/FAILED 后收到成功回执：不自动改为 PAID，返回非法终态并进入人工异常处理，避免已重建订单时重复推进。
- 渠道预下单成功但本地 C2 失败：外部订单可能存在，重试应按同一商户单号查询或关闭，不生成另一个本地活跃单。
- 本地关闭成功但渠道关单失败：本地保持 CLOSED，记录外部补偿；若渠道后续成交按上一条异常处理。
- `expire_time` 为空：不参与超时扫描；恰等于当前时间视为已到期。
- 支付单 PAID 但业务推进失败：支付事实不回滚，由独立业务补偿按 `FIRST_PAID`/已付未推进标记处理，不能再次收款。

## 实现提示

所有状态 SQL 都带原状态并检查影响行数；失败后重载决定 `ALREADY_PAID` 或冲突。`uk_active_ref` 兜底活跃唯一性，CLOSED/FAILED 与清键同条更新。费率、渠道查询和业务资格由应用层提供；支付聚合只接收确定输入。渠道成交时间优先写 `pay_time`，不使用本地回调处理时间替代。
