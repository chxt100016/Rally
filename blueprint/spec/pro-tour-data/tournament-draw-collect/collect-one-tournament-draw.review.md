# pro-tour-data.tournament-draw-collect.flow.collect-one-tournament-draw 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 指定赛事入口的鉴权、参数、赛事查找和成功响应是什么？
  > GET /tour/collect/draws 位于 /tour/collect/** 普通鉴权排除范围。tournamentId 必填且原样查询；repository 只按 tournamentId LIMIT 1，不带 year。查无返回 null，facade 随后解引用而 OPERATION_FAILED。成功 Controller void，HTTP 空响应体，无计数。
  → 已写入触发、接口契约、详细流程第 1/7 步及入口异常分支

- [Q2] ATP/WTA 与大满贯如何路由来源，来源为空或非单打如何处理？
  > ATP: category=GS→ATP_APP_DRAW，否则 ATP_DRAW；WTA: GS→ATP_APP_DRAW，否则 WTA_DRAW，随后一律 ATP_APP_COMPLETED。tour 用 enum valueOf，category 用 equals，未知/空会异常。客户端空结果形成空 draw 列表不更新。shouldCollect 默认排除 DOUBLES，只有 tour.collect.doubles=true 才放行，但仍取决于转换器是否产出双打。
  → 已写入详细流程第 2-3 步、异常分支与路由技术线索

- [Q3] 签表、比赛、球员和参赛信息按什么身份保存，字段与状态如何覆盖？
  > 签表键 tournamentId+year+drawType，存量仅用非空 size/totalRounds 更新。比赛先挂 drawId，按 drawId+matchId 保存，现有字段只以新快照非空值覆盖，但状态若非空可回退；关键 tournamentId/year/matchId 缺失整批拒绝。球员按 tour+playerId，参赛按 drawId+playerId，参赛只非空覆盖 seed/entryType，既有 status 保留。来源遗漏对象不删除。
  → 已写入详细流程第 4-6 步、业务活动与服务边界

- [Q4] WTA 已完成赛果补充、事务边界及中途失败后的部分数据如何收场？
  > 每个 WTA 目标在主签表来源后继续 ATP_APP_COMPLETED；该客户端校验返回 tournamentId/year 与参数一致，不符则空结果。draw、matches、players、entries 分别进入各自事务/保存调用，不存在覆盖单赛事的总事务。后一步失败不回滚前步；指定入口未捕获异常，直接失败且不补偿。
  → 已写入详细流程第 6-7 步及部分提交异常说明
