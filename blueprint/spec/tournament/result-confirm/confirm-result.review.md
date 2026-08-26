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

- [Q3] 全员确认后的报名结算、轮次推进和并发如何处理？
  > 完成后按 winnerEntryNo 和报名 stage/比赛 round 结算胜负，再调用 advanceIfReady；比赛用版本更新防并发，冲突整体回滚。本流程不登记订阅信息或发送通知。
  → 已写入详细流程第 5-6 步、并发异常与事务线索
