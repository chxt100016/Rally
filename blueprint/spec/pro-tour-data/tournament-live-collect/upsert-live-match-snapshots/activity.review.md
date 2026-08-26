# pro-tour-data.tournament-live-collect.activity.upsert-live-match-snapshots 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 存量比赛的覆盖、状态回退和来源遗漏规则是什么？
  > 只有来源非空字段覆盖存量，状态允许回退；空字段保留旧值，来源遗漏比赛不删除。
  → 已写入活动契约、详细流程第 3-4 步与边界情况

- [Q2] 比赛批次失败对签表、此前赛事和后续调度阶段有何影响？
  > 比赛批次回滚但已提交签表和此前赛事保留，后续赛事停止；定时外层仍继续 OOP、DRAW 阶段。
  → 已写入异常分支、详细流程第 5 步与边界情况

- [Q3] 实时状态如何映射，未知状态如何处理？
  > S/Scheduled/U→PENDING，C→COMING，P→LIVE，F/Completed→FINISHED；未知状态映射为 null。
  → 已写入详细流程第 1 步与活动契约
