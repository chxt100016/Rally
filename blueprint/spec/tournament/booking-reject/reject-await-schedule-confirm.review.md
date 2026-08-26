# tournament.booking-reject.flow.reject-await-schedule-confirm 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 等待对方确认时谁可拒赛，为什么要求发起人的确认状态已为 CONFIRMED？
  > 仅当前比赛的订场人可发起，且其参与关系 confirmStatus 必须为 CONFIRMED；这共同表明订场人已确认并正在等待另一方。比赛须为 SCHEDULED，本人须属于参与者；否则报 TOURNAMENT_WAITING_SCHEDULE_CONFIRM_REJECT_FORBIDDEN。
  → 已写入触发、详细流程第 2 步与阶段异常

- [Q2] 确认等待期从哪个时间点起算，缺失或未到期如何处理？
  > 从 scheduleSubmittedTime 起算，读取 TOURNAMENT_MATCH_REJECT_TIMEOUT_HOURS，缺省 48 小时；提交时间缺失或当前时间未到截止点均报 TOURNAMENT_MATCH_REJECT_TOO_EARLY，不修改数据。
  → 已写入详细流程第 3 步、超时异常与技术线索

- [Q3] 成功拒赛后的终态、报名和赛约清理、并发与通知如何处理？
  > 本人确认改为 REJECTED 并记时间，比赛改为 REJECTED 并记理由；仅关闭 DRAFT 赛约，仅把 IN_MATCH 报名退回 WAITING。版本冲突或持久化失败整体回滚；提交后通知异步容错。
  → 已写入详细流程第 4-6 步、持久化异常与技术线索
