# tournament.result-confirm-admin.activity.advance-tournament-progress 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 本活动是否只在 confirm-result-by-admin 交付 matchCompleted=true 时才执行，决赛与非决赛的分支判据是什么？
  > 只在 matchCompleted=true 时执行；用已完成比赛自身的 round 是否为 FINAL 判定分支，不用赛事 currentRound 代替，与自助确认赛果活动一致
  → 触发条件与详细流程

- [Q2] 非决赛时是否复用 @tournament.round-progress 领域服务评估目标轮次，再用 @tournament.tournament 的单向推进命令写入？
  > 复用：先调用 round-progress 评估，ADVANCE 时取 targetRound 调用赛事聚合单向推进命令；NOT_READY/STAY 不写入，与自助确认赛果活动一致
  → 领域依赖与业务动作

- [Q3] 决赛完成时是否直接调用 @tournament.tournament 的完成赛事命令（C8），入参取自 confirm-result-by-admin 交付的 winnerEntryNo 和 completedTime？
  > 是：决赛完成时直接调用完成赛事命令，winnerEntryNo 和 completedTime 均取自上游活动交付的结果，不重新读取比赛
  → 领域依赖 @tournament.tournament 与业务动作
