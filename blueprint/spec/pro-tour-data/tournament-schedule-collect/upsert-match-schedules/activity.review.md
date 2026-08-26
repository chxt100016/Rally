# pro-tour-data.tournament-schedule-collect.activity.upsert-match-schedules 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] “随后”场次和时区如何计算？
  > 带时区时间转 Asia/Shanghai；同场随后场次，Tennis TV ATP 每场加 100 分钟，其余 Schedule 来源每场加 70 分钟。
  → 已写入业务动作 A1 与详细流程第 1-2 步

- [Q2] 比赛身份与字段覆盖规则是什么？
  > 按 drawId+matchId；同批同键后到非空合并，存量仅非空覆盖，状态可回退，遗漏不删。
  → 已写入活动契约与详细流程第 3-4 步

- [Q3] 解析或保存失败如何处理？
  > 可降级字段置 null 不清存量；关键身份/冲突/批量保存失败回滚比赛，签表保留并停止后续赛事。
  → 已写入异常分支、详细流程第 5 步与边界情况
