# tournament.unmatched-entry-elimination.activity.eliminate-unmatched-entry-units 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 跨报名与比赛实例筛选候选是否登记为新的领域服务 @tournament.unmatched-entry-elimination？
  > 是。登记 @tournament.unmatched-entry-elimination 领域服务，负责跨报名和在途比赛快照判定完整候选参赛单元。
  → 落入领域依赖：新增跨实例候选判定领域服务。

- [Q2] 完整参赛单元是否按 SINGLE 一人、DOUBLE 两人且双向搭档与共享 entryNo 一致判定？
  > 是。SINGLE 必须恰好一人；DOUBLE 必须恰好两人、互为 partnerId 且共享 entryNo，否则整组排除。
  → 落入领域依赖、详细流程与边界情况：按赛制检查成员完整性。

- [Q3] 是否只淘汰赛事当前轮次的 WAITING/FROZEN，其他轮次和状态全部排除？
  > 是。只淘汰 currentRound 等于赛事当前轮次且状态为 WAITING/FROZEN 的完整单元。
  → 落入触发条件、业务动作与详细流程：限定当前轮次 WAITING/FROZEN。

- [Q4] 候选在保存前发生轮次、状态或在途比赛变化时是否整批回滚？
  > 是。轮次、候选状态或在途比赛关系并发变化时不允许部分淘汰，整批回滚并提示刷新。
  → 落入异常分支和实现提示：并发变化整批回滚。

- [Q5] 活动是否先按 tournamentId+userId 锁定目标报名，再只查询该用户在本赛事的进行中比赛参与关系，而不加载全赛事报名和全部比赛？
  > 是。先校验赛事，再按 tournamentId+userId 锁定单条报名；只查询该 userId 在本赛事进行中比赛的参与关系，不再加载全赛事报名和全部比赛。
  → 落入时序图、A2-A4 和性能边界。

- [Q6] 未入赛判定是否以 userId 的参与关系为准，不使用 entryNo 推导或联动双打搭档？
  > 是。未入赛只按目标 userId 判断，不用 entryNo 扩展到搭档；最终也只更新目标报名。
  → 落入领域依赖、详细流程和双打边界。

- [Q7] 目标报名存在但轮次不符、状态不符、仍在进行中比赛三类失败是否分别返回报名状态非法与独立的 IN_ACTIVE_MATCH 错误，并且都不修改搭档？
  > 是。报名轮次或状态不符返回 TOURNAMENT_ENTRY_STATUS_INVALID；存在进行中比赛返回 TOURNAMENT_ENTRY_IN_ACTIVE_MATCH；两类都不修改目标或搭档报名。
  → 落入异常分支和事务边界。
