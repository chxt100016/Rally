# transaction-payment.receipt-recovery.activity.reconcile-payment-status 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 扫描阈值与范围是什么？
  > CALLBACK/RECEIVED 且 createTime<当前减五分钟，全量无分页排序。
  → 已写入触发条件与详细流程第 1 步

- [Q2] 哪些回执/订单会短路？
  > 非 ORDER/空 ref 无需查；订单非 PENDING 也不查渠道、不补业务。
  → 已写入活动契约、异常分支与详细流程第 2-3 步

- [Q3] 渠道未付后会不会继续自动重查？
  > 不会；本轮最终会标 PROCESSED，之后不再进入 RECEIVED 扫描。
  → 已写入详细流程第 4-5 步与边界情况
