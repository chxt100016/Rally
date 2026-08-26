# pro-tour-data.player-tournament-path-query.flow.query-player-tournament-path 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权、四个必填参数及签表或球员不存在时如何响应？
  > GET /tour/player/tournament 在普通 AuthInterceptor 排除清单，匿名可用。tournamentId/year/playerId/drawType 都是必填 query 参数；缺失或 year 转换失败走全局失败。字符串不 trim、不归一化。签表或球员查无时 Result.ok(data=null)。
  → 已写入接口契约、详细流程第 1-2 步及参数异常分支

- [Q2] 球员比赛如何排序，晋级路径、出局信息和比分如何判定？
  > 球员比赛按 roundNumber null→0 升序且无稳定次序。仅精确 FINISHED 进入完赛判断：winnerId 等于球员为 WIN 并加入 progressPath，否则在 winnerId 非空且不等时判出局，循环中最后败局覆盖 eliminationInfo。比分按 sets 当前数组顺序以本人视角拼接，无盘分为“已完成”。
  → 已写入详细流程第 4-6 步、流程图及组装异常分支

- [Q3] 未出局时 next 与更远潜在对手如何选择，哪些球员不会成为候选？
  > 未出局先取排序后第一场非 FINISHED 且 matchIndex 非空的比赛；有另一方才形成 next。否则用最后一场胜局，从其相邻签表子树找未淘汰且 seed 最小者。后续逐层同法选择；无种子、已淘汰或子树中不存在的球员不会成为候选，缺候选的轮次直接跳过。
  → 已写入详细流程第 7-8 步与服务边界

- [Q4] 球员、种子、国家地区、姓名、年龄和轮次资料的查询与降级规则是什么？
  > 主球员只按 playerId .one() 查询，未带 tour；种子按当前 draw+playerId。对手资料同样只按 playerId 合并，姓名仅 lastName，缺资料回退 playerId。对手种子按 tournamentId 跨年份/签表合并并取先到值。国家未知回退原码，年龄按当前日，轮次未知时中文标签空，预测轮次可由 matchIndex 推算。
  → 已写入详细流程第 2-5、9 步及数据缺失降级说明

- [Q5] 排期与球场如何展示，翻译覆盖哪些字段，缺译文有何副作用？
  > next 排期优先 scheduledAt（月日+LocalTime），再 matchDate（月日），再 scheduledAtText，随后追加 court 与 courtSeq，全缺为“待定”。翻译覆盖主球员完整姓名、所有路径对手显示名，以及仅 next 的 court；球场译文只替换 next.score 中原名，court 字段仍保留原文。缓存 miss 保留原文并逐条尝试登记待译。
  → 已写入详细流程第 7、10 步、业务活动及翻译异常说明

- [Q6] 读取、组装、比分或翻译失败时是否返回部分结果，已登记待译项是否回滚？
  > 签表/球员读取、.one() 多行、比赛/参赛资料读取、DTO/比分组装或翻译缓存未处理异常会终止整体并由全局处理为 OPERATION_FAILED，不返回部分主体。待译保存逐条 catch，单条失败不影响响应；异常前已成功登记的待译记录无共享事务，不回滚。
  → 已写入异常分支、流程图与待译保存补充说明
