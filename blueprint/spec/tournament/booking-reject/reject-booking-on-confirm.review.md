# tournament.booking-reject.flow.reject-booking-on-confirm 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 如何从同一 schedule-confirm 接口中识别直接拒赛分支，理由组合必须满足什么条件？
  > 必须 confirm=false，rejectReason 与 rebookReason 恰有一个；本流程要求 rejectReason 非空且 rebookReason 为空。两者同空或同非空报 TOURNAMENT_INVALID_REJECT_REASON；confirm=true 则属于确认分支。
  → 已写入接口契约、选择异常与服务边界

- [Q2] 谁能直接拒赛，拒赛次数按什么赛段和上限检查、何时递增？
  > 比赛须为 SCHEDULED，当前用户须有所属赛事报名且属于参与者。按报名 stage 选择 qualifierRejectLimit 或 mainDrawRejectLimit，已有次数达到上限时报 TOURNAMENT_REJECT_LIMIT_REACHED；只有成功拒赛时递增对应赛段次数。
  → 已写入详细流程第 2-4 步、次数异常与技术线索

- [Q3] 直接拒赛是否需要等待超时，成功后的比赛、报名、赛约、并发和通知如何处理？
  > 直接拒赛不检查等待期限，也不要求本人原确认状态。本人和比赛改为 REJECTED，当前赛段拒赛次数加一；仅关闭 DRAFT 赛约，仅将 IN_MATCH 报名退回 WAITING。版本冲突或持久化失败整体回滚；提交后通知异步容错。
  → 已写入详细流程第 4-6 步、持久化异常与服务边界
