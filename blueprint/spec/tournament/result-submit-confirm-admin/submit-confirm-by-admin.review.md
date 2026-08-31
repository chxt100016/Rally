# tournament.result-submit-confirm-admin.flow.submit-confirm-by-admin 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 比赛不是 PENDING_PLAY/PENDING_CONFIRM 时的失败码，沿用旧的 TOURNAMENT_INVALID_RESULT_CONFIRM 还是换新码？
  > 沿用 TOURNAMENT_INVALID_RESULT_CONFIRM
  → 异常分支：状态非 PENDING_PLAY/PENDING_CONFIRM 时报 TOURNAMENT_INVALID_RESULT_CONFIRM

- [Q2] winnerEntryNo 不合法（不是本场比赛参赛编号之一）的失败码怎么定？
  > 新增 TOURNAMENT_RESULT_WINNER_INVALID
  → 异常分支：winnerEntryNo 不是本场比赛参赛编号之一时报 TOURNAMENT_RESULT_WINNER_INVALID

- [Q3] 覆盖已提交胜方（PENDING_CONFIRM 场景 winnerEntryNo 与原记录不同）时，是否需要单独的失败码/提示区分“首次提交”与“覆盖”，还是两者统一按成功处理、不做区分？
  > 不区分，统一按成功处理
  → 详细流程与分支表：覆盖已提交胜方与首次提交均按同一成功提示返回，不新增失败码或分支
