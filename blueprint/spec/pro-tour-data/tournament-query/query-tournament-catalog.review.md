# pro-tour-data.tournament-query.flow.query-tournament-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权以及 status、type、range 的精确筛选规则是什么？
  > GET /tour/tournament/tournaments 在普通鉴权排除清单，三个参数都可省略。status 区分大小写：FINISHED→completed，ONGOING/UPCOMING→active，其他/null 不筛。type 原样精确匹配且不校验。range 忽略大小写：recent=今日±1月交集；live=强制 active 且赛期覆盖今日；其他不筛日期。
  → 已写入接口契约、详细流程第 1-2 步与未知条件分支

- [Q2] 类别过滤、排序、分页与空结果口径是什么？
  > DB 按 startDate 升序，不分页无上限。之后 category 空/blank 保留；trim 后能解析整数则仅保留>=250；非数字保留。数据库无结果或过滤后为空均成功返回 []，不做分组、签名或翻译。
  → 已写入详细流程第 2-3 步、接口契约和空结果分支

- [Q3] 同期分组和 groupId 如何生成，空城市或空赛期如何处理？
  > 按查询顺序遍历，每项只与各组首项比较：两项原始 city lowerCase 相等且赛期闭区间相交才入组；null city 变空串，因此两个 null/空城市可相等，但任一 start/end null 都不能加入已有组。每组按 startDate nullsLast 排序；groupId 为展平时 result.size()+1 形成 g1、gN，非稳定标识。
  → 已写入详细流程第 4-5 步及分组技术线索

- [Q4] 展示状态、返回字段、年份与背景图签名如何交付？
  > 每项返回 id/name/type/typeLabel/category/surface/surfaceLabel/city/startDate/endDate/status/statusLabel/groupId/backgroundUrl，不返回 year；id 仅 tournamentId。今日>end→FINISHED，否则今日<start→UPCOMING，否则 ONGOING，缺端点可能落 ONGOING。backgroundPath 非空白即生成 3600 秒签名 URL，不检查对象存在。
  → 已写入接口契约、详细流程第 5-6 步及背景图说明

- [Q5] 翻译覆盖哪些文本，缓存未命中或查询异常如何收场？
  > 以原始赛事 name、city、surfaceLabel 查询 TOURNAMENT/CITY/SURFACE 的 zh-CN 缓存，命中非空译文替换；miss/空缓存保留原文并逐条尝试新增待译记录，单条保存异常被吞掉。赛事读取、DTO/签名生成或缓存查询未处理异常终止整体 OPERATION_FAILED；异常前已登记记录不回滚。
  → 已写入详细流程第 7 步、业务活动与翻译异常分支
