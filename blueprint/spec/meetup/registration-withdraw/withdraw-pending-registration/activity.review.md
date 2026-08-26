# meetup.registration-withdraw.activity.withdraw-pending-registration 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 活动报名查询包含哪些状态，零条、多条和 JOINED 如何处理？
  > 按 meetupId+userId 查询唯一 PENDING/JOINED；零条报 NOT_JOINED，多条唯一查询失败，JOINED 报 WAITLIST_NOT_PENDING，其他历史不参与。
  → 已写入异常分支、业务动作 A1-A2、详细流程第 1-3 步与边界情况

- [Q2] 是否核实约球状态、时间、到期与主记录存在？
  > 不读取约球主表，不核实约球存在、状态、时间、joinMode 或 expiresAt；只要报名残留且 PENDING 即可撤回。
  → 已写入触发条件、详细流程第 3 步与边界情况

- [Q3] 状态、optTime、人数、通知与并发覆盖如何处理？
  > 按 bizId 二次读取后置 WITHDRAWN、optTime=now，不改人数、群聊、通知且不通知发布者。更新无原状态/版本条件，与审批或拒绝并发时后写可能覆盖。
  → 已写入领域依赖、业务动作 A3、详细流程第 4-5 步、边界情况与实现提示
