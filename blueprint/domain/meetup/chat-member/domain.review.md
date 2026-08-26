# @meetup.chat-member 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 聚合边界是一条成员记录，还是同一 refId 下的完整聊天成员集合？
  > 同一 refId 下的成员集合是聚合边界，refId 是频道标识，每条成员记录是内部实体。这样发布消息时可在一次事务中递增其他成员并推进发送者。
  → 聚合根、实体、边界与 I4

- [Q2] 新加入成员是否要把加入前的历史消息计入初始未读数？
  > 不计入。新成员 unreadCount=0、lastReadMessageId 为空，加入前历史消息仍可查询，但不进入未读统计。
  → I2、C1 与新成员边界情况

- [Q3] 发送消息与真实拉取消息对 lastReadMessageId、lastReadTime 和 unreadCount 的推进语义如何区分？
  > 两者都只允许 lastReadMessageId 单调向前；发送自己的消息把本人 unreadCount 校准为该位置后的数量但不更新 lastReadTime，只有真实拉取并前进时更新 lastReadTime。
  → I3、C3、C4 与发送/拉取边界情况

- [Q4] 发布者或拉取者缺少成员记录时是否补建，主动加入遇到既有关系时是否幂等？
  > 发布或拉取可补建缺失成员以修复阅读状态；显式加入命令遇到既有关系仍返回 ALREADY_JOINED_CHAT。离开命令对不存在关系按幂等成功。
  → 状态、C1-C4 与重复加入/缺失关系边界情况
