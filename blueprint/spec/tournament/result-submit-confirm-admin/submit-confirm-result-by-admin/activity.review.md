# tournament.result-submit-confirm-admin.activity.submit-confirm-result-by-admin 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] C6 提交赛果需要一个提交人 userId，用哪个参与者？
  > 胜方参赛编号（winnerEntryNo）下任一参与者的 userId
  → 业务动作/详细流程：取 winnerEntryNo 对应参与者列表中任一 userId 作为提交人调用 C6

- [Q2] 比赛已是 PENDING_CONFIRM 且 winnerEntryNo 与原记录不同（覆盖场景），领域层怎么实现？
  > 扩展 @tournament.match 的 C6（提交赛果）前置状态为 PENDING_PLAY 或 PENDING_CONFIRM，允许重新提交覆盖胜方并重置全员确认
  → 领域依赖：@tournament.match 提交赛果命令前置状态扩展为 PENDING_PLAY/PENDING_CONFIRM；需回到 domain 层落实

- [Q3] 已被记为 REJECTED 的参与者，本次代确认是否覆盖？
  > 保持原状，不覆盖，与产品文档一致
  → 详细流程/边界情况：C7 只处理仍为 PENDING 的参与者，REJECTED 保持原状
