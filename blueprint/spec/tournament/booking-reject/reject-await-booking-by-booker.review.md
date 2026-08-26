# tournament.booking-reject.flow.reject-await-booking-by-booker 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁可以在订场阶段通过这个入口拒赛，必须满足哪些身份和状态条件？
  > 比赛必须为 BOOKING；当前用户须属于比赛参与者且等于 courtBookerId。非订场人报 TOURNAMENT_NOT_COURT_BOOKER，不额外检查报名状态、赛段或拒赛次数。
  → 已写入触发、详细流程第 2 步与身份异常

- [Q2] 订场等待期从何时开始，发生过重订时如何重新计算？
  > 起点为 courtBookerSelectedTime 与 lastRebookTime 中较晚者，读取 TOURNAMENT_MATCH_REJECT_TIMEOUT_HOURS，缺省 48 小时；起点缺失或未到期报 TOURNAMENT_MATCH_REJECT_TOO_EARLY。
  → 已写入详细流程第 3 步、超时异常与技术线索

- [Q3] 成功拒赛后的状态、赛约关闭、报名释放、并发和通知如何处理？
  > 本人参与关系和比赛改为 REJECTED，比赛记录理由；仅关闭 DRAFT 赛约，仅将 IN_MATCH 报名退回 WAITING。版本冲突或持久化失败整体回滚；提交后通知异步容错。
  → 已写入详细流程第 4-6 步、持久化异常与技术线索
