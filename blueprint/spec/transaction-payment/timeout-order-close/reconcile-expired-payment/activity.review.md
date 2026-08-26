# transaction-payment.timeout-order-close.activity.reconcile-expired-payment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 到期条件是否包含等于当前时刻？
  > 不包含；必须 PENDING、expireTime 非空且严格早于 now。
  → 已写入触发条件、详细流程第 1 步与边界情况

- [Q2] 查单错误何时关闭或保留 PENDING？
  > 非成功或特定 ServiceException 走关闭；不支持、SDK/未转换异常通常保留 PENDING 重试。
  → 已写入活动契约、异常分支与详细流程第 2-4 步

- [Q3] 扫描与并发有什么限制？
  > 全量无分页排序，扫描后状态可能变化，后续条件更新结果又未可靠检查。
  → 已写入详细流程第 1、4 步与边界情况
