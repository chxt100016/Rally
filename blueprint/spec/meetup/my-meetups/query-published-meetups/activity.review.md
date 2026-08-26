# meetup.my-meetups.activity.query-published-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 我发布是否依赖报名、时间、类型和状态？
  > 只要求 creator_id=userId 且 status!=DRAFT，不依赖报名，不限制 NORMAL/TOURNAMENT、时间或其他状态。
  → 已写入触发条件、业务动作 A1-A2、详细流程第 1-2 步与边界情况

- [Q2] 草稿和已过期 OPEN 如何处理与展示？
  > DRAFT 排除；已过 end_time 的 OPEN 仍查询返回，卡片层只把主标签显示为 FINISHED，不更新 status 字段或数据库。
  → 已写入详细流程第 1、4 步、边界情况与实现提示

- [Q3] 排序分页与并发变化有哪些边界？
  > 按 biz_id 倒序、lastId 严格小于、LIMIT size+1，不按开始/更新时间且 total=null；跨页状态变化可改变页长。
  → 已写入活动契约、业务动作 A2-A3、详细流程第 3-4 步与边界情况
