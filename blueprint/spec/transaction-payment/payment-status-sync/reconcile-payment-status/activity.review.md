# transaction-payment.payment-status-sync.activity.reconcile-payment-status 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些本地状态会查渠道？
  > 只有 PENDING；PAID/CLOSED/FAILED 均直接短路。
  → 已写入活动契约与详细流程第 2-3 步

- [Q2] 渠道未确认和渠道不可用有何区别？
  > 非成功/可转换查单错误保持 PENDING 并正常 UNPAID；不支持或 SDK 不可用报错。
  → 已写入异常分支与详细流程第 3 步

- [Q3] 用户同步的事务边界是什么？
  > 支付确认与业务推进同应用事务，异常外抛回滚本地变化，但微信付款事实保留。
  → 已写入详细流程第 4 步与边界情况
