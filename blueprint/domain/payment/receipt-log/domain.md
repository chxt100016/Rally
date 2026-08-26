---
id: "@payment.receipt-log"
kind: aggregate
tables:
  - name: payment_log
    columns: [id, biz_id, channel, log_type, ref_type, ref_id, raw_body, process_status, remark, create_time, update_time]
---

## 概要

记录一次支付链路事件的不可变报文及单向处理结论。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 支付回执日志 | 日志业务编号 `biz_id` | 每次留痕时由雪花编号生成 | 渠道、日志类型、关联单号、脱敏报文和处理结果 | `payment_log` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 关联对象 | 可选关联类型与关联编号 | `ref_type`、`ref_id` |
| 处理结论 | 处理状态与可选摘要 | `process_status`、`remark` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 支付单引用 | `ref_type=ORDER` 时的 `ref_id` | `@payment.payment-order` | 只保存商户单号，不装载或修改支付单 |

## 边界

一次加载与保存的单位是一条支付链路日志。身份、渠道、类型、关联和报文建立后不可变；CALLBACK 只允许从收到状态终结一次。

渠道验签解密、支付单确认、业务推进和恢复任务都在边界外。只有验真成功的内容才能建立 CALLBACK 日志；验真前失败不伪造已认证留痕。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `RECEIVED` | 已验真并留痕，后续业务尚未给出终结结论 | `PROCESSED`、`FAILED` | `C2` |
| `PROCESSED` | 无需推进或处理完成的终态 | 无 | 无 |
| `FAILED` | 处理失败并保存摘要的终态 | 无 | 无 |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 日志编号非空唯一；渠道、日志类型非空，建立后身份、关联和报文不可修改 | 日志根、关联对象 | 审计记录若可改写，同一编号将代表不同渠道事实，无法追溯回调处理 | `PAYMENT_LOG_IMMUTABLE` |
| I2 | `CALLBACK` 建立时必须为 `RECEIVED`；`COLLECT/PREPAY` 是纯留痕，建立即 `PROCESSED` | 日志根、处理结论 | 类型和初态若分开保存，恢复扫描会误处理建单日志或漏掉真实回调 | `PAYMENT_LOG_INITIAL_STATE_INVALID` |
| I3 | `RECEIVED` 只能原子迁移一次到 `PROCESSED/FAILED`；`FAILED` 必须有非空且不超过 255 字符的摘要 | 日志根、处理结论 | 回调与恢复任务可能并发终结，同一日志不能同时显示成功和失败 | `PAYMENT_LOG_STATE_CONFLICT` |
| I4 | `raw_body` 必须是验真后可用内容的脱敏快照，不得包含密钥、签名凭据或不必要个人敏感字段 | 日志根 | 原始报文用于审计但长期持久化，必须在同一次建立时完成脱敏，之后不可补救性改写 | `PAYMENT_LOG_SENSITIVE_CONTENT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 记录支付事件 | 日志尚不存在 | 渠道、类型、可选关联、验真并脱敏的报文 | CALLBACK 为 `RECEIVED`，COLLECT/PREPAY 为 `PROCESSED` | 类型或渠道非法；CALLBACK 未验真；内容未完成脱敏 |
| C2 | 终结回调处理 | `RECEIVED` | `PROCESSED` 或 `FAILED` 结论、可选摘要 | 指定终态 | 原状态已变化；FAILED 摘要为空；摘要超过 255 字符 |

恢复扫描只查询达到等待阈值的 `CALLBACK/RECEIVED`，查询本身不改变日志；每条恢复仍通过 C2 条件终结。

## 边界情况

- 渠道验签或解密失败：不执行 C1；由入口安全日志记录，不能把未经验证的报文写成支付事实。
- 同一商户单号收到重复回调：每次验真成功都建立独立日志；支付确认幂等由支付单聚合保证。
- 非交易、非成功或缺商户单号的已验真事件：建立 RECEIVED 后可终结为 PROCESSED，不推进支付单。
- 回调处理异常：C2 置 FAILED；终态不自动重开，微信重试会形成新的 CALLBACK 日志。
- 进程在建立 RECEIVED 后中断：恢复任务可在阈值后处理并终结。
- 回调与恢复并发：只有一个 C2 原状态条件更新成功，另一方重载终态后结束。
- rawBody 为空：允许用于无法取得正文但已获得可信事件元数据的留痕；不能因此跳过关联和状态校验。
- 终结写入失败：日志保持 RECEIVED，等待下一轮恢复，不把内存结论当作已持久化。

## 实现提示

仓储仅允许 insert 与 `WHERE biz_id=? AND process_status='RECEIVED'` 的条件终结，并检查影响行数。原始正文进入聚合前执行结构化脱敏；remark 只保存短错误分类和安全摘要。扫描使用 `log_type/process_status/create_time` 条件并分页，避免全量装载。
