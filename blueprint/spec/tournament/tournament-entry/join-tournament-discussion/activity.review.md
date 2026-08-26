# tournament.tournament-entry.activity.join-tournament-discussion 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 已有孤立讨论成员如何处理？
  > 报 ALREADY_JOINED_CHAT，保留孤立记录，但回滚本次报名和搭档变化。
  → 已写入活动契约、异常分支与详细流程第 1-2 步

- [Q2] 新成员阅读状态如何初始化？
  > lastRead 为空、unreadCount=0、joinedAt 为当前时间，不补历史未读。
  → 已写入业务动作 A2-A3、详细流程第 3 步与边界情况

- [Q3] 该活动失败是否保留报名？
  > 不保留；它与报名和搭档关系同事务，持久化失败整体回滚。
  → 已写入异常分支与详细流程第 4 步
