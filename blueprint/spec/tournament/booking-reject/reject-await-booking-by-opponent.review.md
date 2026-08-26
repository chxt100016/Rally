# tournament.booking-reject.flow.reject-await-booking-by-opponent 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁属于等待订场的对方，缺少订场人或由订场人本人调用时如何处理？
  > 比赛必须为 BOOKING 且 courtBookerId 非空；当前用户须属于参与者并且不能等于 courtBookerId。缺少订场人或由订场人调用均报 TOURNAMENT_WAITING_BOOKING_REJECT_FORBIDDEN，不额外检查报名状态、赛段或拒赛次数。
  → 已写入触发、详细流程第 2 步与阶段异常

- [Q2] 对方的等待期从何时开始，重订是否会重置等待起点？
  > 起点同订场人入口，为 courtBookerSelectedTime 与 lastRebookTime 中较晚者，所以每次重订会以新的 lastRebookTime 重置等待；配置缺省 48 小时，起点缺失或未到期均报过早。
  → 已写入详细流程第 3 步、超时异常与技术线索

- [Q3] 拒赛成功后的比赛、参与者、报名、赛约、并发和通知语义是什么？
  > 本人参与关系与比赛改为 REJECTED，记录时间和理由；仅关闭 DRAFT 赛约，仅将 IN_MATCH 报名退回 WAITING。版本冲突或持久化失败整体回滚；提交后通知异步容错。
  → 已写入详细流程第 4-6 步、持久化异常与技术线索
