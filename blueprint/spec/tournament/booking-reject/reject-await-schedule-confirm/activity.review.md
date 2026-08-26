# tournament.booking-reject.activity.reject-await-schedule-confirm 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁可以在确认等待超时后拒赛？
  > 仅 SCHEDULED 比赛的订场人，且本人已 CONFIRMED 并属于参与者。
  → 已写入触发条件与详细流程第 1 步

- [Q2] 超时从何时开始计算？
  > 固定以 scheduleSubmittedTime 为起点，缺失或未到配置期限均不修改。
  → 已写入活动契约、异常分支与详细流程第 2 步

- [Q3] 拒赛时本人确认、赛约和报名如何变化？
  > 本人确认也改 REJECTED；仅关闭 DRAFT 赛约，在赛报名回 WAITING，全部同事务。
  → 已写入业务动作 A2-A3、详细流程第 3-5 步与边界情况
