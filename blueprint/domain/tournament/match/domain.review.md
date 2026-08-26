# @tournament.match 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 比赛参与关系是否属于比赛聚合内部？
  > 属于。rally_tournament_match 是聚合根，rally_tournament_match_participant 是参与者实体；阶段推进必须与两套确认状态同事务保存。
  → 订场提交、重订、赛果提交和确认活动都要求根与全部参与关系原子更新。

- [Q2] 状态图是否包含表注释未列出的 PENDING_PLAY？
  > 包含。全员确认赛约后 SCHEDULED→PENDING_PLAY，提交胜方后 PENDING_PLAY→PENDING_CONFIRM，最终进入 COMPLETED。
  → booking-confirm 与 result-submit 的可执行契约明确依赖 PENDING_PLAY，旧列注释不完整。

- [Q3] 什么状态允许物理删除比赛？
  > 仅 MATCHED 或 BOOKING 可由运营未订场取消命令条件删除根和参与者；SCHEDULED 及以后只能按业务状态推进或拒绝。
  → 批量取消活动逐场复核这两个状态，并以条件删除防止并发误删。

- [Q4] 赛约确认和赛果确认能否共用一个确认状态？
  > 不能。confirmStatus 只服务赛约；resultConfirmStatus 只服务赛果。重订重置前者，提交结果重置后者，二者互不覆盖。
  → 参与者表和各活动分别维护两套状态与时间，生命周期触发点不同。
