# meetup.registration-approve.activity.join-approved-participant-chat 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 新成员初始字段和历史消息如何处理？
  > 生成雪花成员编号，unreadCount=0、joinedAt=当前时间，lastReadMessageId/lastReadTime 为空；不读取审批前历史消息。
  → 已写入领域依赖、业务动作 A1-A2、详细流程第 1-3 步与边界情况

- [Q2] 已有成员和并发唯一冲突是否按幂等成功？
  > 不按幂等成功。既有 refId+userId 抛 ALREADY_JOINED_CHAT，并发唯一冲突按保存失败。
  → 已写入异常分支、详细流程第 1、4 步与边界情况

- [Q3] 群聊失败与审批事务的回滚边界是什么？
  > 成员写入与 PENDING->JOINED 及人数重算同一事务；检查或保存失败整体回滚。
  → 已写入触发条件、业务动作 A3、详细流程第 4 步与实现提示
