# pro-tour-data.finished-matches-query.flow.query-finished-matches 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 接口鉴权、tournamentIds 输入和一分钟缓存键规则是什么？
  > GET /tour/match/finished 在 AuthInterceptor 排除清单内，匿名可用。tournamentIds 必须出现但无格式/数量/去重/年份校验；完整 List 作为 Caffeine key，顺序和重复影响命中，成功 DTO 缓存1分钟。
  → 接口契约、详细流程第 1-2、11 步与技术线索

- [Q2] 种子如何补资料、判断淘汰和分入 ATP/WTA/OUT，多赛事间是否隔离？
  > 按签表取 seed 非空非0，按 playerId 补资料、按 tournamentId 补 tour，seed 升序。任一 FINISHED 比赛中 winnerId 非空即把每个不等于 winnerId 的对阵方按 playerId 记为淘汰；不校验 winner 属于对阵。淘汰不按赛事隔离且多次落败最后遍历轮次覆盖。淘汰进 OUT；未淘汰仅 ATP 值进 ATP，其余都进 WTA。
  → 详细流程第 3-5 步与异常补充说明

- [Q3] 完赛比赛的状态、对阵、球员缺失、比分和排序规则是什么？
  > 只精确 status=FINISHED；双方 playerId 均空过滤，单方保留。球员资料缺失时比赛球员 name=playerId、国家为空。startedAt 倒序 null 最后，无次级排序。sets_json 空白变空列表，非法 JSON 中断整体；盘字段可空，末盘双方局数齐全才生成当前盘比分。
  → 详细流程第 6-8 步与读取/比分异常分支

- [Q4] 轮次如何分组和排序，未知轮次与同时间比赛如何处理？
  > 按 round 原值分组，固定 F/SF/QF/R16/R32/R64/R96/R128 顺序；未知 round 的中文名为空，排在最后并保持其在 startedAt 已排序比赛中的首次出现顺序。同 startedAt 或都 null 时无稳定次序。
  → 详细流程第 6、9 步与接口契约

- [Q5] 简中翻译如何应用，缓存未命中是否产生持久化副作用？
  > 对比赛球场、比赛球员和种子姓名查 zh-CN 缓存，非空译文替换；miss 保留原文并逐条尝试新增空 translated_text 记录，保存异常吞掉。成功 DTO 缓存后1分钟内不重新翻译/登记；若后续翻译步骤失败，之前已保存待译项不回滚。
  → 详细流程第 10-11 步、业务活动与翻译异常分支

- [Q6] 空数据、部分赛事命中与解析/读取失败时是否返回部分结果？
  > 未知赛事或无数据分别形成空 seed/match 列表，部分赛事命中则合并有数据项且不占位。任一 DB/映射/非法 sets_json/翻译未处理异常终止整个调用，不交付已组装部分；待译保存自己的单条异常除外。
  → 接口契约、详细流程第 11 步与完整异常分支
