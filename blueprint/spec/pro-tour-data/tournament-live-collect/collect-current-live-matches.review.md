# pro-tour-data.tournament-live-collect.flow.collect-current-live-matches 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] HTTP 入口鉴权、目标赛事窗口、空范围和响应是什么？
  > GET /tour/collect/live 位于 /tour/collect/** 鉴权排除范围，无参数。目标为 startDate<=today+1 且 endDate>=today-1 的全部赛事，不看 status。空范围直接结束；Controller void，成功 HTTP 空响应体，不能区分空、跳过和成功。
  → 已写入触发、接口契约、详细流程第 1/6 步及空范围分支

- [Q2] ATP/WTA 单打过滤、来源空值与赛事一致性如何处理？
  > 每项都调用 ATP_APP_LIVE。TourEnums 选择 ATP→MS、WTA→LS，其他值抛错。响应/data/liveMatches 空或前缀过滤后空形成空结果继续下一赛事；响应 eventId/year 必须匹配参数，否则整批丢弃。HTTP 获取空可跳过，但 JSON parse 等未捕获异常会终止。
  → 已写入详细流程第 2-3 步与来源空值异常分支

- [Q3] 实时状态与逐盘比分如何映射，未知或不完整数据如何降级？
  > 状态映射 S/Scheduled/U→PENDING，C→COMING，P→LIVE，F/Completed→FINISHED，未知→null。盘分要求双方 team 存在且第一方 sets 非空；第一方 setNumber null/0 或 setScore null 跳过。第二方同盘缺失时仍保留第一方，P2 分数/抢七为 null；无有效盘则 sets null。
  → 已写入详细流程第 4 步及未知状态/盘分补充说明

- [Q4] 签表和比赛如何保存，单项异常是否继续后续赛事，部分提交如何收场？
  > 先按 tournamentId+year+drawType 保存签表，size/totalRounds 都为 null，存量结构不变；再按 drawId+matchId 批量事务保存比赛，关键身份缺失整批失败，存量只非空/非空白覆盖。签表与比赛事务独立。liveMatch 没有逐赛事 catch，任一异常终止后续赛事；此前赛事和新签表保留。
  → 已写入详细流程第 5-6 步、业务活动和部分提交异常分支
