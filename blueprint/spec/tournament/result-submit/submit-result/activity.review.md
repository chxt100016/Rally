# tournament.result-submit.activity.submit-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁能提交何种胜方？
  > PENDING_PLAY 的任一参与者可提交，winnerEntryNo 必须属于本场参赛单元。
  → 已写入触发条件、异常分支与详细流程第 1-2 步

- [Q2] 提交后比赛和参与确认如何重置？
  > 写胜方、提交人、时间并转 PENDING_CONFIRM；提交人 CONFIRMED，其余 PENDING 且清原时间。
  → 已写入活动契约、业务动作 A3-A4 与详细流程第 3-4 步

- [Q3] 提交成功是否已结算且通知授权失败怎么办？
  > 尚未结算报名；授权过滤或登记异常只记录，不回滚赛果提交。
  → 已写入详细流程第 5 步与边界情况
