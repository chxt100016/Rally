# pro-tour-data.tournament-schedule-collect.activity.upsert-schedule-draw 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些赛事和来源会进入签表保存？
  > 赛期与 [昨日,明日] 相交且来源产出目标单打才进入；未知 tour、空来源或无目标单打跳过，空 category 可能失败。
  → 已写入触发条件、异常分支与详细流程第 1-2 步

- [Q2] 签表身份和刷新规则是什么？
  > 按 tournamentId+year+drawType；新增或以来源非空 size/totalRounds 刷新，空字段保留存量。
  → 已写入活动契约与详细流程第 3 步

- [Q3] 失败如何影响后续步骤和调度阶段？
  > 签表独立事务；本活动失败终止后续赛事，成功后其他步骤失败不补偿签表；定时外层继续 DRAW。
  → 已写入异常分支、详细流程第 4 步与边界情况
