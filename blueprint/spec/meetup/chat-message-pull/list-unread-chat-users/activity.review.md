# meetup.chat-message-pull.activity.list-unread-chat-users 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 未读用户候选范围包含哪些报名状态，是否包含当前用户和创建者？
  > 基于 meetup 的有效参与者报名，仅 JOINED、REVIEWED、SKIPPED；排除当前用户。创建者只有在其报名记录也处于这些状态且不是当前用户时才进入候选。PENDING 不进入未读列表。
  → 已写入 reads、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 无消息、无 chat_user、空已读位置和已退出聊天分别如何判未读？
  > 先取约球最新消息 bizId；无消息返回空列表。候选没有 chat_user（从未加入或已退出）视为未读，lastReadTime=null；记录存在但 lastReadMessageId 为空或小于最新 bizId 也未读。
  → 已写入业务动作 A2、详细流程第 3-4 步与边界情况

- [Q3] 用户资料缺失、结果排序和 withUnreadUsers=false 如何返回？
  > 只有流程请求 withUnreadUsers=true 才执行；否则 unreadUsers=null。批量查未读用户资料，缺失时 nickname/avatarUrl 为空但保留该用户；结果沿候选参与者/未读判定顺序，不承诺额外排序。
  → 已写入触发条件、活动契约、业务动作 A3-A4、详细流程第 5-6 步
