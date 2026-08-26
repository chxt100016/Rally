# meetup.meetup-invite.activity.join-invited-participant-chat 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 群聊成员建立哪些初始字段，是否基于历史消息计算未读？
  > 以 meetupId/refId 和 inviteeUserId 建 ChatUserData，生成雪花 bizId，unreadCount=0，joinedAt=当前时间；lastReadMessageId/time 空。不读取历史消息，邀请前消息不计初始未读。
  → 已写入领域依赖、业务动作 A2、详细流程第 2-3 步与边界情况

- [Q2] 已有群聊成员、已退出后重邀和并发创建如何处理？
  > 先按 refId+userId exists，存在报 ALREADY_JOINED_CHAT；退出会删除记录，因此可重建。表有 ref_id+user_id 唯一键，并发时一个保存可能唯一冲突归 SYSTEM_ERROR/既有异常，不自动幂等。
  → 已写入异常分支、业务动作 A1-A2、详细流程第 1、4 步与边界情况

- [Q3] 本活动失败如何影响已建立报名和人数？
  > 依赖 register 活动后执行，处于同一外层事务；ALREADY_JOINED_CHAT 或保存异常会回滚刚创建报名、currentPlayers 和聊天写入。成功无业务返回。
  → 已写入触发条件、详细流程第 5 步与边界情况
