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
