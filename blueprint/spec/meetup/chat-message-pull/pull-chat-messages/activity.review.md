# meetup.chat-message-pull.activity.pull-chat-messages 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 参与资格的精确状态和约球不存在如何处理？
  > 先加载 meetup；不存在报 MEETUP_NOT_FOUND。创建者直接通过；其他用户的报名为 PENDING、JOINED、REVIEWED 或 SKIPPED 任一即可，REJECTED/WITHDRAWN/QUIT 不可进入并报 NOT_JOINED。
  → 已写入异常分支、领域依赖、业务动作 A1 与详细流程第 1 步

- [Q2] 无游标、错误游标、limit 与消息排序的边界是什么？
  > lastMessageId 空白时先找倒序跳过最近 200 条后的那条 bizId 作为下界，因此首拉最多覆盖最近 200 条；非空游标不验证归属或存在，直接用 biz_id > 游标。按 biz_id 升序；limit 默认20，正数才 LIMIT，0/负数不限制。
  → 已写入活动契约、业务动作 A2、详细流程第 2-4 步与边界情况

- [Q3] 已读位置的新建、前进、未读重算及部分失败怎么处理？
  > 空消息批不改状态。有消息取最后一条 bizId；无 chat_user 就创建，lastReadTime/joinedAt=当前时间，unreadCount=该位置后消息数；已有记录仅新位置字符串比较更大时先更新位置和时间，再重算未读。无总事务，第二次更新失败可能位置已推进但未读数未校准。
  → 已写入领域依赖、业务动作 A3、详细流程第 5-7 步与边界情况
