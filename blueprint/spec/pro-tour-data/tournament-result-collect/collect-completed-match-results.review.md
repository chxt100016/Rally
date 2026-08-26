# pro-tour-data.tournament-result-collect.flow.collect-completed-match-results 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权、参数和成功文案是什么？当前成功路径是否可达？
  > GET /tour/collect/completed 位于 /tour/collect/** 鉴权排除范围，tournamentId/year 必填且 year 须为整数。Controller 设计为成功返回“已完成比赛采集完成”，但当前所有参数合法的调用也会因 tour 缺失而 OPERATION_FAILED，所以完成文案不可达。
  → 已写入触发、接口契约、详细流程第 1/3 步及当前失败分支

- [Q2] tour 为什么为空，异常发生在上游请求前还是后，是否会写数据？
  > Controller new TournamentData 只 set tournamentId/year，Facade.completed 直接构造 DrawParams(..., tournament.getTour())，故 tour=null。AbstractMatchCollectClient.fetch 先执行 request(params)，即可能已发出上游 HTTP；随后 switch(TourEnums.valueOf(null)) 抛异常。此时尚未生成 MatchCollectResult，Manager 的签表/比赛保存循环未开始，无本地写入。
  → 已写入详细流程第 2-3 步、异常分支与必现失败技术线索

- [Q3] 若补齐 tour，来源空、错赛和 MS/LS 过滤如何处理？
  > 有效 ATP tour 走 ms 并保留 MS 前缀，有效 WTA 走 ls 并保留 LS。response/data 空、matches 空、过滤后空都返回空结果；响应 eventId/year 需数字匹配请求，否则整批拒绝。Manager 无 draw 可遍历，因此不写数据，若入口可达则返回完成文案。
  → 已写入详细流程第 4 步及不可达空来源分支

- [Q4] 若补齐 tour，状态、盘分、签表和比赛如何保存与覆盖？
  > 状态 S/Scheduled/U→PENDING、C→COMING、P→LIVE、F/Completed→FINISHED、未知→null。盘分以第一方为基准，setNumber null/0 或第一方 setScore null 跳过，第二方同盘缺失可保留半边。签表键赛事+年+MS/LS、结构为空；比赛键 drawId+matchId，批内重复以后到非空合并，存量仅非空/非空白覆盖，状态可回退。
  → 已写入详细流程第 5-6 步与下游覆盖补充说明

- [Q5] 下游事务失败时有哪些部分提交，当前 HTTP 入口是否可能出现这些状态？
  > Draw saveOrUpdate 与 Match saveOrUpdateBatch 是独立事务；若比赛身份/约束/保存失败，比赛批回滚但新建签表保留。来源遗漏不删、空值不清旧字段。当前 HTTP 入口在任何保存前必然失败，所以不会产生这种部分提交；只有内部调用传入有效 tour 时才可能。
  → 已写入详细流程第 6 步、服务边界及不可达部分提交分支
