# meetup.meetup-quit.flow.quit-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名状态和约球生命周期允许退出，发布者本人能否退出自己的约球？
  > 按 Java 实现确认：findActiveRegistration 可找到 PENDING 或有效报名，但 canQuit 只接受 JOINED、REVIEWED、SKIPPED。普通约球不检查状态与时间，DRAFT、OPEN、ONGOING、FINISHED、CLOSED 都可退出；没有禁止 creator，其 JOINED 报名也可改为 QUIT。赛事约球单独拒绝。
  → 已落入详细流程、异常分支和服务边界。

- [Q2] 临近开始处罚如何计算，当前是否实际读取处罚分值或扣减信誉分？
  > 按 Java 实现确认：用 Duration.between(now,startTime).toHours 与 meetup.quit.penalty_threshold_hours 比较，小于阈值返回 PENALIZED，包括开始后的负数；应用层只有 TODO，不读取 meetup.quit.penalty_under_6h、不调用评分域、不返回结果。
  → 已落入详细流程、成功响应和技术线索。

- [Q3] 本人群聊成员已经不存在时，退出事务如何处理，历史消息是否删除？
  > 按 Java 实现确认：chatDomainService.quit 直接按 refId+userId 删除，记录不存在也不报错；不删除 chat_message。它与报名及人数保存位于同一事务，底层失败才会回滚。
  → 已落入详细流程、群聊活动和异常分支。

- [Q4] 退出人资料缺失和退出通知失败分别是否回滚报名、人数与群聊变更？
  > 按 Java 实现确认：报名、人数、群聊变更后会读取退出人 UserProfile；资料不存在抛 TOKEN_INVALID，同一事务整体回滚。notify 在事务提交后异步执行并吞掉触发与发送异常，通知失败不回滚；创建者退出时通知收件人仍是创建者本人。
  → 已落入流程图、异常分支和通知说明。
