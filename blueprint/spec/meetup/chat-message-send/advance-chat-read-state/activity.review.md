# meetup.chat-message-send.activity.advance-chat-read-state 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 其他成员未读数更新范围是什么，无 chat_user 的参与者怎么办？
  > 对 ref_id=meetupId 的所有现有 chat_user 中 user_id!=senderId 执行 unread_count=unread_count+1；不按报名状态过滤，不为没有 chat_user 的参与者建记录。
  → 已写入领域依赖、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 发送者已读位置如何推进或建立，是否可能回退？
  > 用新消息 bizId。发送者无 chat_user 时创建，lastReadMessageId=新消息、lastReadTime/joinedAt=当前时间、unreadCount=该位置后消息数；已有记录仅当新 ID 字符串更大时推进并重算，绝不回退。
  → 已写入业务动作 A2、详细流程第 3-4 步与边界情况

- [Q3] 消息保存与两段阅读状态更新之间的事务和失败补偿是什么？
  > 消息已由上游保存。先递增他人未读，再推进发送者位置；没有覆盖活动间和本活动多次写入的显式总事务。任一步失败报 SYSTEM_ERROR，不删除消息、不回滚已经完成的未读递增或位置更新；重试发送会新建另一条消息。
  → 已写入异常分支、详细流程第 5-6 步与边界情况
