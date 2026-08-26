# tournament.result-reject.flow.reject-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 赛果拒绝如何从同一确认接口分流，比赛、身份、理由和次数上限如何校验？
  > 同一 POST /result-confirm 仅 confirm=false 进入本服务；比赛必须 PENDING_CONFIRM、本人须有报名并在参与者中。rejectReason 必填；按报名 stage 选 qualifier/mainDraw 上限，当前次数必须严格小于上限，不要求本人确认状态仍为 PENDING。
  → 已写入触发、接口契约、详细流程第 1-3 步及资格异常

- [Q2] 拒绝后比赛、确认状态、拒绝计数、报名和关联赛约分别怎么变化？
  > 本人 resultConfirmStatus=REJECTED 并写时间，比赛=REJECTED、保存 reason code，保留原 winner/submitted 字段；本人对应阶段拒绝数+1。仅关闭 DRAFT 赛约；同场仅 IN_MATCH 报名回 WAITING，轮次等字段不变。
  → 已写入详细流程第 4-5 步、容错分支与服务边界

- [Q3] 并发保存和拒绝通知失败如何处理？
  > 比赛以版本更新，冲突或任一持久化失败整体回滚。拒绝通知按比赛事件注册为事务提交后异步发送；未订阅记 SKIPPED，渠道失败记 FAILED，均不改变拒绝、回池与赛约结果。
  → 已写入详细流程第 6 步、并发/通知异常与技术线索
