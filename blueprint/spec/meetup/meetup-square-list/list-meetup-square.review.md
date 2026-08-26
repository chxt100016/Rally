# meetup.meetup-square-list.flow.list-meetup-square 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] TIME、DISTANCE 和 RECOMMEND 三种排序的实际查询与分页方式分别是什么？
  > 按 Java 实现确认：TIME 在数据库用 (startTime,bizId) 升序 search-after 并 LIMIT pageSize+1；DISTANCE 在数据库计算全部符合项的球面距离并排序，再按 bizId 在内存切 pageSize+1；RECOMMEND 直接返回空列表。
  → 已落入详细流程、业务活动和流程图。

- [Q2] 续页标识为空、非法、字段数量不足、时间格式错误或记录已消失时如何处理？
  > 按 Java 实现确认：空白、解码失败或非 JSON 数组由 parseCursor 返回空列表，按首页。只有一个字段时 TIME 缺 lastStartTime，也按首页；第二字段时间格式非法会在 LocalDateTime.parse 抛系统异常。DISTANCE 找不到 lastBizId 时从首页；TIME 游标对应记录消失仍可按时间和 bizId 下界继续。
  → 已落入请求参数、详细流程和异常分支。

- [Q3] 水平模式、单边水平、标签、满员和赛事约球是否参与广场筛选？
  > 按 Java 实现确认：levelMode 与 tags 完全未使用；只有 levelMin、levelMax 同时存在时做区间交集。查询不排除已满员，不按当前用户准入，也不筛 meetupType，因此 OPEN 且未结束的普通和赛事约球都可出现。
  → 已落入请求参数、详细流程和服务边界。

- [Q4] 页大小、半径和经纬度实际有哪些缺失、范围和上限校验？
  > 按 Java 实现确认：Bean Validation 只要求 pageSize>=1，默认 10；显式 null 会在应用层拆箱比较时系统异常，没有最大值。DISTANCE 必须有 lng/lat；TIME 仅 radiusKm 非 null 时要求 lng/lat。经纬度、半径不校验范围或正数，负半径通常得到空页。
  → 已落入请求参数和异常分支。
