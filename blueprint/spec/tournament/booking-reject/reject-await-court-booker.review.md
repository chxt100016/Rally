# tournament.booking-reject.flow.reject-await-court-booker 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁可以在待选订场人阶段发起拒赛，比赛与参与关系必须满足什么条件？
  > 任一比赛参与者均可发起；比赛必须存在且仍为 MATCHED，当前用户必须能在参与关系中找到。不要求其为订场人，也不检查本人报名状态、赛段或拒赛次数。
  → 已写入触发、详细流程第 2 步与服务边界

- [Q2] 48 小时等待期从哪个时间点计算，配置和过早请求如何处理？
  > 从 matchedTime 起算，读取 TOURNAMENT_MATCH_REJECT_TIMEOUT_HOURS，缺省 48 小时；stageStartedAt 为空或当前时间早于截止点均报 TOURNAMENT_MATCH_REJECT_TOO_EARLY，且不修改数据。
  → 已写入详细流程第 3 步、超时异常与技术线索

- [Q3] 拒赛成功后比赛、参与者、报名、关联赛约和通知分别如何处理？
  > 本人参与关系改为 REJECTED 并记确认时间，比赛改为 REJECTED 并记理由；仅关闭仍为 DRAFT 的关联赛约，仅把 IN_MATCH 报名退回 WAITING。版本冲突或持久化失败整体回滚；提交后通知异步且内部容错，不影响成功结果。
  → 已写入详细流程第 4-6 步、持久化异常与技术线索
