# meetup.chat-message-pull.flow.pull-chat-messages 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 待审核报名者为何可以拉取消息，却不进入未读用户统计范围？
  > 按 Java 实现确认：Meetup.assertIn 用 findActiveRegistration，接受 PENDING 或有效参与状态；未读统计则用 getActiveParticipantIds，只保留 JOINED、REVIEWED、SKIPPED，因此待审核者可拉取但不会被列为未读用户。
  → 已落入触发、详细流程与未读用户活动边界。

- [Q2] 无游标和非正数 limit 的实际查询边界是什么？
  > 按 Java 实现确认：无游标时先定位到最近 200 条之前，随后仍按升序和 limit 拉取；Controller 默认 limit=20。Repository 仅在 limit>0 时追加 LIMIT，所以零或负数会返回起点后的全部消息，且没有上限校验。
  → 已落入请求参数、详细流程和异常分支后的边界说明。

- [Q3] 拉取消息推进已读位置时发生分步写入失败，如何保证或恢复一致性？
  > 按 Java 实现确认：没有显式事务或补偿。已有记录先更新已读位置，再统计并更新未读数；后一步失败可能留下位置已推进但未读数未校准。新记录则先完成计数再保存；整体失败对外按系统异常。
  → 已落入触发、异常分支与流程图。
