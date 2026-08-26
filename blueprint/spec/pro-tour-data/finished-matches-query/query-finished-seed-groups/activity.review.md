# pro-tour-data.finished-matches-query.activity.query-finished-seed-groups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 种子范围与淘汰映射如何建立？
  > 取输入赛事签表中seed非null且非0的报名；FINISHED有winner比赛把另一侧playerId记为淘汰轮次，不校验winner属于双方，也不按赛事隔离。
  → 已写入业务动作 A1-A2、详细流程第 1-2 步与边界情况

- [Q2] ATP、WTA、OUT 如何分类？
  > 命中淘汰映射即ELIMINATED进OUT；其余ACTIVE，tour忽略大小写等于ATP进ATP，其他全部进WTA。
  → 已写入业务动作 A3 与详细流程第 3 步

- [Q3] 分组和组内排序如何返回？
  > 仅返回非空组，顺序ATP/WTA/OUT；组内seed升序，同号无稳定次序。
  → 已写入活动契约、详细流程第 4 步与边界情况
