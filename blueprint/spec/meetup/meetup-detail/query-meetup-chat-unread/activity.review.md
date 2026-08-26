# meetup.meetup-detail.activity.query-meetup-chat-unread 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些 actionState 可查询未读，其他状态如何返回？
  > 只有 JOINED、ONGOING_JOINED、OWNER_EDITABLE、OWNER_EDIT_LOCKED、FINISHED_JOINED、FINISHED_REVIEWED、CLOSED_JOINED 可进入聊天并执行；其他状态 unreadCount=null。
  → 已写入触发条件、活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 有无 chat_user 时未读数口径是什么，是否推进已读？
  > 按 meetupId+userId 查 chat_user；存在直接返回冗余 unread_count；不存在则统计该 ref_id 全部 chat_message 数量。纯查询，不创建 chat_user、不更新 lastReadMessageId/time/unreadCount。
  → 已写入 reads、业务动作 A2-A4、详细流程第 2-4 步

- [Q3] 并发新消息、空值与查询失败如何处理？
  > 没有消息且无记录返回0；记录 unread_count 按非空列直接返回。查询期间并发发送可能使结果是某一时点近似值，不做一致性锁；任一查询失败归 SYSTEM_ERROR。
  → 已写入异常分支与边界情况
