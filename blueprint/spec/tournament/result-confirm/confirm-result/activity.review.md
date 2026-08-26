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
