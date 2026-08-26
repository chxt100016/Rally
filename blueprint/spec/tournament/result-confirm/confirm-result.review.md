# tournament.result-confirm.flow.confirm-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 确认入口如何区分确认与拒绝，本人和比赛需要满足哪些条件？
  > 同一 POST /result-confirm 由 confirm 分流；本服务只记录 true，false 属于 result-reject。比赛必须 PENDING_CONFIRM，当前用户须有赛事报名且属于参与者；不校验该报名当前状态/轮次。
  → 已写入触发、接口契约、详细流程第 1-2 步和服务边界

- [Q2] 重复确认与全员确认的精确状态变化是什么？
  > 不要求本人原状态为 PENDING；重复确认会再次置 CONFIRMED 并刷新 resultConfirmTime。只有所有参与者均 CONFIRMED 才要求 winnerEntryNo 非空并将比赛置 COMPLETED、写 completedTime。
  → 已写入详细流程第 3-4 步及状态/胜方异常

- [Q3] 全员确认后的报名结算、轮次推进、并发和通知授权如何处理？
  > 完成后按 winnerEntryNo 和报名 stage/比赛 round 结算胜负，再调用 advanceIfReady；比赛用版本更新防并发，冲突整体回滚。应用层同一事务内随后登记过滤去重的赛事通知授权，登记异常也会导致当前实现事务回滚，而不是保留赛果。
  → 已写入详细流程第 5-6 步、并发异常与事务线索；通知失败口径由 Q4 更正

- [Q4] 更正：通知授权登记内部是否吞掉异常，是否影响赛果事务？
  > NotifySubscribeService.grant 内部捕获并记录所有异常，因此授权登记失败不会向外抛出，也不会回滚赛果确认；主事务继续提交。Q3 最后一处关于异常回滚的判断作废，以本答复为准。
  → 已写入通知授权异常分支与事务线索，明确不影响主流程
