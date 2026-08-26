# personal-profile.self-rating-update.flow.update-self-rating 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 冷却期按什么字段和可信度分档计算，配置缺失或非法时如何处理？
  > 从 ntrpUpdatedAt 到 now 的 ChronoUnit.DAYS 与配置天数比较。credibility null/<30 用 low，30-59 mid，>=60 high。缺配置用枚举默认；整数非法变 0，冷却失效；核查 BigDecimal 阈值非法直接 NumberFormatException。
  → 已在详细流程、契约、异常分支和技术线索明确整日冷却、可信度分档及整数非法按 0、阈值非法失败。

- [Q2] 降低、同值、小幅提高和达到阈值的提高分别是否触发或退出核查期？
  > 只计算 new-old 的正向差额。达到或超过阈值触发；降低、同值或小幅提高不触发，也不把已有 UNDER_REVIEW 状态、isUnderReview 或剩余场次清除。同值仍更新 NTRP 时间并开启新冷却。
  → 已在详细流程、异常说明和服务边界明确只有达到阈值的正向差额触发，其他修改不退出核查。

- [Q3] 触发核查期时哪些字段真正持久化，reviewRemainingMatches 是否保存？
  > 领域对象设置 status=UNDER_REVIEW、isUnderReview=true、reviewRemainingMatches=required，再更新 NTRP 与时间。TennisProfileRepository.update 只写 NTRP、ntrpUpdatedAt、status、isUnderReview 等，不写 reviewRemainingMatches，因此新剩余场次丢失。
  → 已在详细流程、契约和技术线索明确状态与标记持久化、剩余场次未写入。

- [Q4] 每次修改产生哪些日志，日志与档案保存及返回聚合是否同一事务？
  > 达到阈值先写 UNDER_REVIEW 日志；每次成功都写 NTRP 日志，含前值、后值、delta、USER。ProfileAppService.updateNtrp 为事务入口，日志、档案更新和随后 getMyProfile 都在事务中，任一运行时失败整体回滚。
  → 已在业务活动、详细流程和异常分支明确两类日志及档案、聚合共享事务并整体回滚。

- [Q5] 已在核查期或 TBC 状态时是否允许再次修改，成功响应如何展示？
  > 没有按 status 限制，只受档案存在和冷却约束；UNDER_REVIEW 冷却结束可再改并可能再触发，TBC 也可改。TBC 未触发时仍为 TBC，响应只含状态和基础 user，不展示刚更新的 level；触发后变 UNDER_REVIEW 并返回完整分组。
  → 已在触发、详细流程、契约和异常说明明确核查期与 TBC 均可修改及其返回差异。
