# meetup.my-meetups.activity.query-pending-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 待审批、待评价、未读消息三类条件分别是什么？
  > 待审批：本人创建、OPEN/FULL、未结束且存在任意 PENDING 报名；待评价：FINISHED 或 OPEN/FULL 已结束、仍在 deadlineDays 内且本人报名 JOINED；未读：非 DRAFT、本人 chat_user unread_count>0 且本人是创建者或 JOINED/REVIEWED/SKIPPED。
  → 已写入业务动作 A1-A4 与详细流程第 1-4 步

- [Q2] 同一约球命中多个原因时如何合并、排序和分页？
  > 三支 UNION 包含不同 pendingReason，故同一约球命中不同原因时保留多行且无优先级。整体按 biz_id 倒序、lastId 严格小于、LIMIT size+1；相同 bizId 跨页边界可能遗漏另一原因。
  → 已写入活动契约、业务动作 A5、详细流程第 5-6 步与边界情况

- [Q3] 评价期限配置异常、状态大小写和查询副作用如何处理？
  > review.deadline_days 解析失败按 0；SQL 只认大写 OPEN/FULL/FINISHED/DRAFT。活动只读，不审批、评价或清未读。
  → 已写入异常分支、详细流程第 1、7 步、边界情况与实现提示
