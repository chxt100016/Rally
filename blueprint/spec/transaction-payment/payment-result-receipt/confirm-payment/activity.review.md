# transaction-payment.payment-result-receipt.activity.confirm-payment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 不同本地状态如何处理回执？
  > PENDING 确认 PAID；PAID 为重复幂等且不推进；CLOSED/FAILED 报非法。
  → 已写入活动契约、异常分支与详细流程第 2-3 步

- [Q2] 首次确认记录哪些字段？
  > 写 PAID、channelTransactionId 与本地处理 payTime。
  → 已写入业务动作 A3 与详细流程第 3 步

- [Q3] 条件更新并发结果是否可靠检查？
  > 没有；只允许 PENDING 的更新影响行数被忽略，并发时仍可能被当作首次推进。
  → 已写入详细流程第 4 步与边界情况
