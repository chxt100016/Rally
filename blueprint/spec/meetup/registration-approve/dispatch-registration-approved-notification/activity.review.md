# meetup.registration-approve.activity.dispatch-registration-approved-notification 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 满员与未满员分别选择什么场景和接收人？
  > 审批后人数>=上限选 TEAM_SUCCESS 给全部有效参与者，否则选 JOIN_SUCCESS 只给获批申请人；两个分支互斥。
  → 已写入触发条件、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 是否只在首次满员通知，成员复核和额度消费如何执行？
  > 不判断首次满员，已满/超员后再次审批仍触发 TEAM_SUCCESS。提交后每用户选首条 UNUSED、成员复核后 CAS 到 SENDING；明确退出跳过，复核异常 fail-open。
  → 已写入业务动作 A2-A3、详细流程第 2-4 步与边界情况

- [Q3] 异步失败、幂等、重试与事务回滚如何处理？
  > 无额度、CAS、渠道、发送或回写失败仅记录，不影响审批；无通知级幂等键或持久化重试。事务回滚则 afterCommit 不执行。
  → 已写入异常分支、详细流程第 5-6 步、边界情况与实现提示
