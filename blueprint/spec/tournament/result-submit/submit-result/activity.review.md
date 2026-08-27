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

- [Q3] 本活动是否结算报名或处理通知？
  > 尚未结算报名，也不登记订阅信息或发送通知；这些都不属于赛果提交活动。
  → 已写入详细流程第 5 步与边界情况

- [Q4] 赛果提交权限是否继续允许本场任一参与者操作，不要求提交人属于 winnerEntryNo 对应参赛单元？
  > 是。只要求提交人是本场参与者，不限制其属于胜方参赛单元；继续保持现有权限边界。
  → 已落实到活动契约、业务动作 A1、详细流程第 1 步与边界情况。

- [Q5] winnerEntryNo 不属于本场时是否继续复用 TOURNAMENT_RESULT_WINNER_REQUIRED，而不新增专门的非法胜方错误码？
  > 是。winnerEntryNo 为空或不属于本场均继续映射为 TOURNAMENT_RESULT_WINNER_REQUIRED，不改变既有错误码契约。
  → 已落实到活动契约、异常分支、业务动作 A2 与详细流程第 2 步。

- [Q6] 提交后是否继续把提交人直接设为 CONFIRMED，并把其他所有参与者重置为 PENDING 且清空原 confirmedTime？
  > 是。提交人直接 CONFIRMED 并写 submittedTime，其他所有参与者重置为 PENDING 且清空 confirmedTime。
  → 已落实到 @tournament.match、业务动作 A4、详细流程第 4/5 步与边界情况。
