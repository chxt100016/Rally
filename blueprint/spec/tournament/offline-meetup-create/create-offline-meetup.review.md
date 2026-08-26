# tournament.offline-meetup-create.flow.create-offline-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时允许创建，候选参赛者如何选取，是否要求赛事 ACTIVE？
  > 仅检查 currentRound==offlineFromRound 且 offlineMeetupId 为空，不检查赛事 ACTIVE 或结束时间。候选为全量报名中 currentRound 等于线下轮次且 status=WAITING 的 userId 去重结果，至少一人，不校验 stage。
  → 已写入详细流程第 2-3 步、候选异常与服务边界

- [Q2] 活动初始状态、成员状态、人数和赛事配置如何映射？
  > 创建 TOURNAMENT 类型、OPEN 状态活动；title/matchType/NTRP 沿用赛事，NTRP 为 EXACT，性别 ANY，加入 APPROVAL，费用空；maxPlayers/currentPlayers 均等于候选人数，每位候选直接为 JOINED。
  → 已写入详细流程第 4-5 步与服务边界

- [Q3] 重复或并发创建、球场引用失败及部分保存失败如何处理？
  > 已有 offlineMeetupId 直接 DATA_DUPLICATE；保存活动和成员后以 bindOfflineMeetupIfAbsent 防并发，失败抛重复并由外层 @Transactional 回滚本次活动和成员。TEXT/MAP courtId 查不到时降级使用请求场地资料，不报错。
  → 已写入详细流程第 6 步、重复/保存异常与事务线索
