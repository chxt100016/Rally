---
id: "@payment.receipt-log"
kind: aggregate
tables:
  - name: payment_log
    columns: [id, biz_id, channel, log_type, ref_type, ref_id, raw_body, process_status, remark, create_time, update_time]
---

## 概要

记录一次支付链路事件的原样报文及处理结论。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 支付回执日志 | 日志业务编号 `biz_id` | 每次留痕时由雪花编号生成 | 渠道、日志类型、关联单号、调用方提供的原样报文和处理结果 | `payment_log` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 关联对象 | 可选关联类型与独立可选的关联编号 | `ref_type`、`ref_id` |
| 处理结论 | 处理状态与可选摘要 | `process_status`、`remark` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 支付单引用 | `ref_type=ORDER` 时可得的 `ref_id` | `@payment.payment-order` | 只保存可得商户单号，不装载或修改支付单；未知、非成功或缺商户单号的回调仍可保存 `ORDER + null/blank ref_id` |

## 边界

一次加载与保存的单位是一条支付链路日志。身份、渠道、类型、关联和报文建立后不由处理命令改写；CALLBACK 的处理状态与摘要按 `biz_id` 普通更新。

渠道验签解密、支付单确认、业务推进和恢复任务都在边界外。只有验真成功的内容才能建立 CALLBACK 日志；验真前失败不伪造已认证留痕。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `RECEIVED` | 已验真并留痕，后续业务尚未给出终结结论 | `PROCESSED`、`FAILED` | `C2` |
| `PROCESSED` | 无需推进或处理完成 | `PROCESSED`、`FAILED` | `C2` |
| `FAILED` | 处理失败并保存调用方给出的摘要 | `PROCESSED`、`FAILED` | `C2` |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 日志编号非空唯一；渠道、日志类型非空，建立后身份、关联和报文不可修改 | 日志根、关联对象 | 审计记录若可改写，同一编号将代表不同渠道事实，无法追溯回调处理 | `PAYMENT_LOG_IMMUTABLE` |
| I2 | `CALLBACK` 建立时必须为 `RECEIVED`；`COLLECT/PREPAY` 是纯留痕，建立即 `PROCESSED` | 日志根、处理结论 | 类型和初态若分开保存，恢复扫描会误处理建单日志或漏掉真实回调 | `PAYMENT_LOG_INITIAL_STATE_INVALID` |
| I3 | 处理状态只能取 `RECEIVED/PROCESSED/FAILED`；FAILED 摘要按调用方值原样保存，可空且不做 255 字符预校验 | 日志根、处理结论 | 领域只表达 main 已有的状态和值，不新增数据库之外的 CAS 或长度约束 | `PAYMENT_LOG_STATE_CONFLICT` |
| I4 | `raw_body` 按调用方提供内容原样保存，可为空；交易回调使用 SDK 解密后的完整交易 JSON，未知事件使用可得原始 body，不在领域内脱敏或清洗 | 日志根 | 审计内容必须与 main 当前实际留痕一致 | `PAYMENT_LOG_IMMUTABLE` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 记录支付事件 | 日志尚不存在 | 渠道、类型、可选关联类型、独立可选的关联编号、调用方提供的原样报文 | CALLBACK 为 `RECEIVED`，COLLECT/PREPAY 为 `PROCESSED` | 类型或渠道非法；非空关联类型非法 |
| C2 | 写入回调处理结果 | 已加载 CALLBACK 日志，不预检当前处理状态 | `PROCESSED` 或 `FAILED` 结论、调用方给出的可选摘要 | 指定状态与摘要，按 `biz_id` 普通更新 | 结论非法；持久化抛出异常 |

恢复扫描只查询达到等待阈值的 `CALLBACK/RECEIVED`，查询本身不改变日志；每条恢复仍通过 C2 普通更新。

## 边界情况

- 渠道验签或解密失败：不执行 C1；由入口安全日志记录，不能把未经验证的报文写成支付事实。
- 同一商户单号收到重复回调：每次验真成功都建立独立日志；支付确认幂等由支付单聚合保证。
- 非交易、非成功或缺商户单号的已验真事件：建立 RECEIVED 后可终结为 PROCESSED，不推进支付单。
- 未知、非成功或缺商户单号的回调仍按 main 保存 `ref_type=ORDER`，`ref_id` 原样允许 null/blank；不强制二者同时提供或同时省略。
- 回调处理异常：C2 置 FAILED；终态不自动重开，微信重试会形成新的 CALLBACK 日志。
- 进程在建立 RECEIVED 后中断：恢复任务可在阈值后处理并终结。
- 回调与恢复并发：C2 不做原状态条件更新、不重载；并发普通更新按数据库实际完成顺序保留最后写入结果。
- rawBody 为空：允许用于无法取得正文但已获得可信事件元数据的留痕；关联类型与编号分别按可得值保存。
- FAILED remark：直接使用异常 `getMessage()`，允许为空或超长；若数据库因列约束拒绝则按普通持久化异常处理，不预先补默认值或截断。
- 终结普通更新忽略影响行数；若仓储抛出异常则向调用方传播，不把内存结论当作已持久化。

## 实现提示

仓储提供 insert 与按 `biz_id` 的普通更新；更新不附加 `process_status='RECEIVED'` 条件，也不依据影响行数补查或重试。`ref_type` 与 `ref_id` 不要求成对非空，rawBody 与 remark 均按调用方值原样交付持久化。扫描使用 `log_type/process_status/create_time` 条件并分页，避免全量装载。
