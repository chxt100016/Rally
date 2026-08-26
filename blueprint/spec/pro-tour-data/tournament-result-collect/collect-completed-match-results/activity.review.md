# pro-tour-data.tournament-result-collect.activity.collect-completed-match-results 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 为什么当前 HTTP 入口没有成功路径？
  > 控制器只传 tournamentId/year，tour 为 null；来源请求后枚举分流必然失败，任何本地保存尚未开始。
  → 已写入概要、活动契约与详细流程第 1 步

- [Q2] 若内部调用补齐 tour，来源选择和跳过规则是什么？
  > ATP 取 MS、WTA 取 LS；空来源、赛事编号/year 不匹配或无目标单打时不写入并正常完成。
  → 已写入异常分支与详细流程第 2 步

- [Q3] 可达保存路径的身份、覆盖和事务规则是什么？
  > 签表按赛事+年份+类型先独立提交；比赛按 drawId+matchId 非空覆盖，遗漏不删，比赛失败可留下签表。
  → 已写入领域依赖、详细流程第 3-6 步与边界情况
