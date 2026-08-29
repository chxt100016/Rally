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

- [Q5] SCHEDULED 内修改关联赛约是否继续只保存赛约，不更新比赛根、参与者确认或 version；只有 BOOKING→SCHEDULED 才走版本条件？
  > 是。SCHEDULED 内编辑仅保存外部赛约，比赛根、参与者与 version 均不变；BOOKING 重新提交才版本更新并重置确认。
  → 已落实到不变量 I6、命令 C3 与边界情况。

- [Q6] 接受赛约时是否继续兼容 meetupId 为空或关联赛约记录缺失，并只在找到赛约且 startTime 严格早于当前时刻时拒绝，时间恰等仍放行？
  > 是。meetupId 为空或记录缺失继续兼容放行；只有读到的 startTime 严格早于当前时刻才报 MEETUP_EXPIRED，恰等不拒绝。
  → 已落实到不变量 I7、命令 C4 与边界情况。

- [Q7] 赛果确认是否继续允许待确认阶段的同一参与者重复确认并刷新时间，且最后一人确认时缺 winnerEntryNo 会使本次根和参与者更新整体回滚？
  > 是。比赛仍待确认时重复确认刷新本人时间；最后确认缺胜方抛错，依靠事务使本次根与参与者变化整体回滚。
  → 已落实到命令 C7 与边界情况。

- [Q8] 新单场取消是否新增独立 C10，而不是放宽既有 C9 未订场批量取消语义？
  > 是。保留 C9 的 MATCHED/BOOKING 旧语义，新增 C10 处理按自然键取消任意非 COMPLETED 比赛。
  → 落入状态、命令与边界情况：新增 C10，不改变 C9。

- [Q9] C10 是否按 tournamentId+matchNo 锁定最新根，并禁止删除最新状态为 COMPLETED 的比赛？
  > 是。按 tournamentId+matchNo 锁定读取最新聚合；最新状态 COMPLETED 时拒绝，其他状态可物理删除。
  → 落入不变量、命令与实现提示：自然键锁定最新根并禁止完成态删除。

- [Q10] C10 成功是否在物理删除前产出 meetupId 与全部参与者 userId/entryNo 的取消快照？
  > 是。删除前返回包含 tournamentId、matchId、matchNo、可选 meetupId 及全部参与者 userId/entryNo 的不可变快照。
  → 落入不变量、命令和边界情况：删除前生成联动快照。
