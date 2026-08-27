# tournament.result-confirm.activity.confirm-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 未全员和全员确认分别如何处理？
  > 本人转 CONFIRMED；尚有人未确认则保持 PENDING_CONFIRM，全员时要求胜方并转 COMPLETED。
  → 已写入活动契约、业务动作 A2-A3 与详细流程第 1-2 步

- [Q2] 资格赛与正赛胜负报名如何结算？
  > 资格赛胜方 PAYING、负方 WAITING；正赛胜方 WAITING 且非决赛晋级，负方 ELIMINATED。
  → 已写入业务动作 A4 与详细流程第 3 步

- [Q3] 缺胜方或轮次保存失败如何收敛？
  > 触发全员确认的本次操作连同本人确认一起回滚；比赛、报名、赛事轮次同事务。
  → 已写入异常分支、详细流程第 4-5 步与边界情况

- [Q4] 同一参与者在比赛仍为 PENDING_CONFIRM 时重复确认，是否继续允许刷新其 confirmedTime，而不是按幂等请求忽略？
  > 是。只要比赛仍为 PENDING_CONFIRM，重复确认继续把本人设为 CONFIRMED 并刷新 confirmedTime；不新增请求级幂等短路。
  → 已落实到业务动作 A2、详细流程第 2 步与边界情况。

- [Q5] 触发全员确认的最后一次请求若发现 winnerEntryNo 为空，是否连本次参与者确认一起整体回滚？
  > 是。全员确认时 winnerEntryNo 必须存在；缺失会抛错并使本次参与者确认及全部结算同事务回滚。
  → 已落实到异常分支、业务动作 A3、详细流程第 3 步与边界情况。

- [Q6] 决赛结束判断是否继续只看已完成比赛自身 round=FINAL，并用 completedTime 同时写赛事 endTime，而不依赖赛事当前轮次字段？
  > 是。只以已完成比赛自身 round=FINAL 判定决赛，用该次 completedTime 写比赛完成时间和赛事 endTime，不额外依赖 tournament.currentRound。
  → 已落实到 @tournament.tournament、业务动作 A5、详细流程第 5 步与边界情况。
