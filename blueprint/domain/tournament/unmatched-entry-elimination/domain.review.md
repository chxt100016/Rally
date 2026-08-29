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
