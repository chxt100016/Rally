# meetup.meetup-join.activity.join-direct-participant-chat 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名状态加入群聊，待审批如何处理？
  > 仅上游状态为 JOINED 时建立本人聊天成员；PENDING 直接跳过，不预建成员。
  → 已写入触发条件、活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 新成员的编号、未读数、加入时间和历史消息如何初始化？
  > 生成雪花 bizId，保存 refId、userId、unreadCount=0、joinedAt=当前时间；lastReadMessageId 和 lastReadAt 为空，不读取或计入加入前历史消息。
  → 已写入领域依赖、业务动作 A2-A4、详细流程第 2-4 步与边界情况

- [Q3] 既有成员、并发唯一冲突和事务回滚如何处理？
  > 存在 refId+userId 时抛 ALREADY_JOINED_CHAT，不按幂等成功；并发由数据库唯一键裁决。活动与上游报名同一事务，成员检查或保存失败会回滚报名和人数。
  → 已写入异常分支、详细流程第 5-6 步、边界情况与实现提示
