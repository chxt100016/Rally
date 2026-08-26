# tournament.booking-reschedule-request.flow.request-rebooking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 重订分支如何与确认和拒赛分支互斥，理由有哪些有效值？
  > 必须 confirm=false，rejectReason 与 rebookReason 恰有一个；本服务要求只有 rebookReason。有效枚举为 TIME_NOT_SUITABLE、PLACE_NOT_SUITABLE、DURATION_NOT_SUITABLE；两者都空或都非空先报 TOURNAMENT_INVALID_REJECT_REASON。
  → 已写入接口契约、选择异常和服务边界

- [Q2] 谁可发起，是否限制本人确认状态或报名状态？
  > 比赛必须 SCHEDULED，当前用户须有该赛事报名并属于比赛参与者；任一参与者包括订场人均可，不要求本人原 confirmStatus=PENDING，也不校验报名 status/stage/round。
  → 已写入触发、详细流程第 2 步与服务边界

- [Q3] 重订成功保留与覆盖哪些比赛/赛约字段，如何处理并发？
  > 比赛退回 BOOKING；保留 courtBookerId、meetupId、旧 scheduleSubmittedTime 和赛约内容，覆盖 lastRebookBy/reason/time；全员 confirmStatus=PENDING、confirmTime=null。版本更新防并发，冲突整体回滚。
  → 已写入详细流程第 3-4 步、并发异常与技术线索
