# transaction-payment.receipt-recovery.activity.advance-paid-business 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时进入恢复业务推进？
  > 仅恢复查询本次把 PENDING 报名费订单确认 PAID 时，既有 PAID 不进入。
  → 已写入触发条件、详细流程第 1 步与边界情况

- [Q2] 席位、报名和赛事怎样推进？
  > 原子占位，PAYING 转 MAIN/WAITING/首轮；资格完成且满位时推进赛事并淘汰剩余资格等待。
  → 已写入活动契约与详细流程第 2-3 步

- [Q3] 恢复路径失败是否强事务？
  > 不是；单条无统一事务，订单 PAID、席位等可能保留，回执随后 FAILED。
  → 已写入异常分支、详细流程第 4 步与边界情况
