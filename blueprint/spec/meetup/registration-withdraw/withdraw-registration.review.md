# meetup.registration-withdraw.flow.withdraw-registration 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 撤回如何查找本人报名，为什么 JOINED 会被查到但随后拒绝，多条活动报名如何处理？
  > 按 Java 实现确认：findActiveByMeetupAndUser 用 meetupId+userId 且 status IN(PENDING,JOINED) 调用 one。无记录返回 null 后报 NOT_JOINED；JOINED 通过查询但 canWithdraw=false 报 WAITLIST_NOT_PENDING；多条匹配时 MyBatis-Plus one 抛 TooManyResultsException，外层转系统异常。
  → 已落入详细流程、流程图和异常分支。

- [Q2] 撤回是否校验约球存在、状态、时间、加入方式或报名自动到期时间？
  > 按 Java 实现确认：RegistrationAppService.withdraw 直接调用 RegistrationDomainService.withdraw，不加载 Meetup，因此不检查约球是否存在、任何约球状态/时间、joinMode 或 registration.expiresAt。
  → 已落入请求参数、详细流程和异常分支后的边界说明。

- [Q3] 撤回写哪些字段，是否改变人数、群聊或通知发布者？
  > 按 Java 实现确认：updateStatus 按 bizId 再读一遍记录，设置 status=WITHDRAWN、optTime=LocalDateTime.now 后 updateById。它不修改 currentPlayers、群聊、约球或其他报名，也不调用通知。
  → 已落入成功响应、详细流程和服务边界。

- [Q4] 撤回与审批通过或拒绝并发时，状态更新是否带原状态条件或版本保护？
  > 按 Java 实现确认：更新按数据库自增 id 执行，不附原 status=PENDING 条件，也没有 version。撤回和聚合审批都可先读到 PENDING，后写可能覆盖先写；没有冲突失败码、优先级或补偿。
  → 已落入触发、详细流程和异常分支后的并发说明。
