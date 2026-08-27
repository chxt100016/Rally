# @meetup.finish-settlement 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 跨多场约球的结算服务是否可以直接批量 UPDATE，还是只输出候选结论？
  > 领域服务保持只读，只筛选并返回应结束的 meetupId；实际状态迁移必须由 @meetup.meetup 的结束命令逐聚合执行，批量 UPDATE 不属于领域服务契约。
  → 职责边界、规则 R5 与实现提示

- [Q2] 哪些存储状态在结束时间已过后属于结算候选，是否继续兼容大小写混用？
  > 统一按语义状态 OPEN、FULL、ONGOING 判定为活跃候选，持久化适配器兼容历史大小写输入；DRAFT、CLOSED、FINISHED 不候选。
  → 规则 R1、R3 与历史状态边界情况

- [Q3] 结束时间恰好等于结算基准时是否进入候选，候选读取是否需要锁表或冻结全量？
  > endTime 小于或等于基准即到期；服务按一次查询快照返回当时的候选，不锁表。后续逐聚合命令会再次校验状态与时间，处理并发变化。
  → 契约 settlementAt、规则 R2/R4 与并发边界情况

- [Q4] 完赛结算应只读返回候选并逐聚合迁移，还是保留 main 的单条批量 UPDATE？
  > 保留 main：单条批量 UPDATE 直接置 FINISHED 并返回 affectedCount，不先读候选、不逐聚合。
  → 职责、输出、R3-R5 与实现提示已改为单条批量更新。

- [Q5] 结算状态应归一化为 OPEN/FULL/ONGOING，还是精确保留 main 的 OPEN 与小写 full？
  > 精确保留 main 的存储字符串口径：只匹配 OPEN 和小写 full，不归一化，也不纳入 ONGOING/大写 FULL。
  → R1 与边界情况已明确精确大小写状态集合。

- [Q6] 结束时间边界应使用 <= 冻结时间，还是保留 main 在构造更新时取 now 并使用严格 <？
  > 保留 main：构造 LambdaUpdateWrapper 时取 LocalDateTime.now()，条件严格使用 end_time < now。
  → 输入、R2 与实现提示已改为运行时 now 和严格小于边界。
