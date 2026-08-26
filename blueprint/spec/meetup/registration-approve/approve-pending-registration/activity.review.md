# meetup.registration-approve.activity.approve-pending-registration 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 审批权限、报名归属状态和允许的约球阶段是什么？
  > registrationId 必须属于加载的 meetup，审批人须 creator，报名须 PENDING，实际状态须非 FINISHED/CLOSED；因此 DRAFT/OPEN/ONGOING 可审批。
  → 已写入触发条件、异常分支、业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 容量、到期时间和申请人准入是否重新校验？
  > 不检查剩余容量、joinMode、expiresAt，也不重查申请人账户、资料、性别、NTRP、信誉或冲突。
  → 已写入详细流程第 3 步与边界情况

- [Q3] 状态、人数、操作时间、重复与并发审批如何处理？
  > 只改 JOINED，不写 optTime/expiresAt，聚合保存后按有效报名重算人数。串行重复报 WAITLIST_NOT_PENDING；并发无名额/版本保护可超员，群聊失败同事务回滚。
  → 已写入业务动作 A3-A4、详细流程第 4-6 步、边界情况与实现提示
