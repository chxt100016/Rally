# @tournament.unmatched-entry-elimination 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 领域服务输出是否同时包含可淘汰 entryNo 与被排除 entryNo，便于调用方复核但不对外返回？
  > 是。返回 candidateEntryNos 和 excludedEntryNos；排除结果只供内部复核，不进入 HTTP 响应。
  → 落入契约与规则：输出候选和排除编号两组内部结果。

- [Q2] 判定在途占用时是否同时按 userId 和 entryNo 保护，任一命中即排除整个单元？
  > 是。同赛事任一在途参与快照的 userId 或 entryNo 命中时，都排除整个 entryNo 单元。
  → 落入规则：userId 或 entryNo 任一在途命中即排除。

- [Q3] 报名列表为空与必填上下文缺失分别如何返回？
  > 报名列表为空返回接受且两个列表为空；matchType、currentRound 或任一必填列表为 null 时返回输入无效拒绝结论。
  → 落入契约与边界情况：空报名接受，必填上下文缺失拒绝。

- [Q4] 候选与排除结果是否按 entryNo 升序稳定输出？
  > 是。候选和排除 entryNo 均去重并按升序稳定输出。
  → 落入规则与实现提示：结果去重并按 entryNo 升序。

- [Q5] 单人判定的输入是否只需要赛事当前轮次、目标报名 userId/status/currentRound，以及该用户是否存在进行中比赛参与事实？
  > 是。只输入赛事当前轮次、目标报名的 userId/status/currentRound，以及该用户是否参与赛事进行中比赛的布尔事实；不扫描其他报名。
  → 落入契约输入与规则 R1-R4：以单个目标报名和 inActiveMatch 事实判定。

- [Q6] 领域服务输出是否分为 ELIGIBLE、ENTRY_STATUS_OR_ROUND_INVALID、IN_ACTIVE_MATCH、INPUT_INVALID 四档，不再返回 entryNo 候选集合？
  > 是。输出仅保留 ELIGIBLE、ENTRY_STATUS_OR_ROUND_INVALID、IN_ACTIVE_MATCH、INPUT_INVALID 四档，不再返回任何 entryNo 候选或排除集合。
  → 落入契约输出与规则 R1-R5：改为单目标判定结果。

- [Q7] 双打 partnerId、entryNo 和搭档状态是否完全不参与单人淘汰判定？
  > 是。partnerId、entryNo 和搭档状态完全不参与单人淘汰判定；只淘汰请求 userId 对应报名，搭档保持原状态。
  → 落入职责边界、契约与边界情况：明确搭档不参与判定且不联动。
