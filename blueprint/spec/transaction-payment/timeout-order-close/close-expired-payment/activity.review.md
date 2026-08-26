# transaction-payment.timeout-order-close.activity.close-expired-payment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 本地和渠道关单顺序是什么？
  > 先本地 CLOSED 并清 activeRefKey，再尽力请求微信关单。
  → 已写入活动契约、业务动作与详细流程第 1-3 步

- [Q2] 渠道关单失败会恢复本地吗？
  > 不会；只记录警告，本地保持 CLOSED 且不再扫描。
  → 已写入异常分支、详细流程第 3 步与边界情况

- [Q3] 并发和关联报名如何处理？
  > 条件关闭影响行数不检查，仍可能请求关单；报名保持 PAYING，可因活跃键释放重建订单。
  → 已写入详细流程第 2、4 步与边界情况
