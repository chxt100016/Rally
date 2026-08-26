# tournament.booking-confirm.flow.confirm-booking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 同一 schedule-confirm 入口如何区分确认、拒赛和重订，本服务处理哪一支？
  > 本服务只处理 confirm=true；confirm=false 且 rejectReason 属 booking-reject，confirm=false 且 rebookReason 属 booking-reschedule-request。比赛必须 SCHEDULED，本人须有赛事报名且在参与者中；不校验报名状态。
  → 已写入触发、接口契约和服务边界

- [Q2] 本人原确认状态有何限制，重复确认或原 REJECTED 如何处理？
  > 不要求本人原 confirmStatus=PENDING；CONFIRMED 重复确认会刷新 confirmTime，REJECTED 也会覆盖为 CONFIRMED。每次按全体当前状态重算是否全员确认。
  → 已写入详细流程第 3-4 步与重复/覆盖分支

- [Q3] 全员确认后比赛与赛约如何变化，缺失/异常赛约和并发如何处理？
  > 全员确认则比赛=PENDING_PLAY；meetupId 为空、活动不存在或非 DRAFT 都不阻止，只有存在且 DRAFT 时改 OPEN。比赛用版本更新；冲突或赛约保存异常整体事务回滚。
  → 已写入详细流程第 4-5 步、赛约容错、并发异常和事务线索
