# tournament.result-reject.activity.reject-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时可拒绝且限额如何累计？
  > 仅 PENDING_CONFIRM 参与者持有效理由；按本人当前资格赛/正赛阶段分别校验并递增计数。
  → 已写入触发条件、异常分支与详细流程第 1-2 步

- [Q2] 拒绝后赛约和报名如何处理？
  > 本人/比赛 REJECTED；仅关闭 DRAFT 赛约，只有 IN_MATCH 报名回 WAITING。
  → 已写入活动契约、业务动作 A3-A4 与详细流程第 3-4 步

- [Q3] 通知和非匹配对象会否阻止成功？
  > 赛约缺失/非 DRAFT、报名非 IN_MATCH 均保持且不阻止；通知不可触达或发送失败写触达日志后容错。
  → 已写入详细流程第 4-5 步与边界情况

- [Q4] 拒赛上限是否继续按拒绝人本人当前报名阶段分别使用 qualificationRejectLimit 或 mainDrawRejectLimit，并在同一事务中只递增对应计数？
  > 是。按拒绝人本人报名阶段选择资格赛或正赛限额；通过校验后仅递增对应阶段计数，并与拒赛结算同事务提交。
  → 已落实到 @tournament.entry、业务动作 A2、详细流程第 2 步与边界情况。

- [Q5] 关联赛约不存在或不是 DRAFT、以及参与者报名已不是 IN_MATCH 时，是否继续视为可兼容状态而不阻止拒赛？
  > 是。赛约缺失或非 DRAFT 时不关闭；报名已非 IN_MATCH 时不回池。这些兼容状态不阻止拒赛，只有参与者报名记录本身缺失才失败。
  → 已落实到 @meetup.meetup、异常分支、业务动作 A4、详细流程第 4 步与边界情况。

- [Q6] 拒赛通知是否继续排除拒绝人本人，以 TOURNAMENT_REJECTED:matchId 去重并在提交后容错发送？
  > 是。通知排除拒绝人，以 TOURNAMENT_REJECTED:matchId 为稳定事件，提交后按接收人与渠道去重并容错发送。
  → 已落实到 @notification.delivery、业务动作 A5、详细流程第 5 步与边界情况。
