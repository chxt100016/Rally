# tournament.booking-confirm.activity.confirm-booking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 重复确认与异常原确认状态如何处理？
  > 本人无论原为 PENDING、CONFIRMED 或异常 REJECTED 均覆盖为 CONFIRMED 并刷新确认时间。
  → 已写入活动契约、详细流程第 2 步与边界情况

- [Q2] 何时推进比赛并开放赛约？
  > 尚有未确认者保持 SCHEDULED；全员确认进入 PENDING_PLAY，且仅关联赛约存在并为 DRAFT 时改 OPEN。
  → 已写入业务动作 A3-A4 与详细流程第 3-4 步

- [Q3] 赛约缺失或并发失败如何收敛？
  > 赛约空/缺失/非 DRAFT 不阻止推进；比赛版本冲突或保存失败使事务整体回滚。
  → 已写入异常分支、详细流程第 4-5 步与边界情况
