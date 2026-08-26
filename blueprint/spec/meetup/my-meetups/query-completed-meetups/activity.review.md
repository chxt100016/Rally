# meetup.my-meetups.activity.query-completed-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 已完成由报名状态还是约球状态与时间决定？
  > 完全由本人报名为 REVIEWED 或 SKIPPED 决定，约球只要求非 DRAFT，不检查 end_time 或 FINISHED。
  → 已写入概要、业务动作 A1-A2、详细流程第 1-2 步与实现提示

- [Q2] 发布者、重复报名和异常提前完成态如何处理？
  > 发布者也必须有 REVIEWED/SKIPPED 报名才入选；子查询 IN 使同一约球的多条完成态报名不复制结果。异常提前变成完成态时即使约球未来 OPEN 也入选。
  → 已写入详细流程第 2-3 步与边界情况

- [Q3] 草稿、分页和 total 的口径是什么？
  > DRAFT 排除；按 biz_id 倒序、lastId 严格小于、LIMIT size+1，仓储判 hasMore，total 固定不统计。
  → 已写入活动契约、业务动作 A2-A3、详细流程第 4 步与边界情况
