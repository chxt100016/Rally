# tournament.single-match-cancel.activity.delete-cancellable-match 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 目标比赛不存在与并发删除是否需要区分错误？
  > 区分。首次按 tournamentId+matchNo 查不到时报比赛不存在；已加载后条件删除未命中时报版本冲突。
  → 落入异常分支与详细流程：首次缺失和加载后删除冲突分档。

- [Q2] REJECTED 是否与其他非 COMPLETED 状态一样允许物理删除？
  > 是。REJECTED 仍属于非 COMPLETED，允许物理删除。
  → 落入触发条件、领域依赖与边界情况：REJECTED 可删除。

- [Q3] 删除是否以最新状态非 COMPLETED 为条件，不要求最初读取的 version 完全不变？
  > 是。锁定并读取最新比赛后，只以最新状态不是 COMPLETED 为删除条件，不要求最初 version 未变化。
  → 落入业务动作、详细流程与实现提示：读取最新状态并按非 COMPLETED 条件删除。

- [Q4] 首次运营终止的条件更新是否按 bizId+expectedVersion 且状态非 COMPLETED/REJECTED 命中，并将 status 更新为 REJECTED、version+1？
  > 是。首次终止按 bizId、expectedVersion 和非 COMPLETED/REJECTED 条件更新 status=REJECTED、version+1；未命中视为并发冲突。
  → 落入 A4、详细流程和并发边界。

- [Q5] 目标已经是 REJECTED 时，活动是否仍加载并返回当前参与者/meetupId 快照，但不再写比赛根？
  > 是。已为 REJECTED 时幂等，不写根也不递增版本，但仍返回当前 meetupId 和全部参与者快照供联动安全重试。
  → 落入 A3-A5、成功返回和边界情况。

- [Q6] 运营终止是否保持 rejectPhase、rejectReasonCode、rejectedBy、rejectedTime 及其他比赛字段原值，不补写拒绝审计字段？
  > 是。只改 status、version 与数据库自动 updateTime；rejectPhase、rejectReasonCode、rejectedBy、rejectedTime 及其他比赛字段全部保留原值。
  → 落入领域依赖、业务动作和实现提示。
