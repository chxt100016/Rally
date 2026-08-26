# tournament.booking-reject.activity.reject-booking-on-confirm 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 直接拒赛与重订如何互斥？
  > confirm=false 时 rejectReason 与 rebookReason 必须恰有一个；本活动要求仅 rejectReason。
  → 已写入触发条件、异常分支与详细流程第 1 步

- [Q2] 拒赛限额如何选择和累计？
  > 依本人报名当前资格赛/正赛阶段选择对应上限，达到即拒绝；成功只递增对应赛段次数。
  → 已写入活动契约、业务动作 A2-A3 与详细流程第 2-3 步

- [Q3] 本人和其他报名如何处理？
  > 本人记录拒赛计数；其他仍在本比赛的报名回 WAITING，DRAFT 赛约按需关闭，事务整体保存。
  → 已写入详细流程第 4-5 步与边界情况
