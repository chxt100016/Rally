# meetup.my-meetups.activity.query-in-progress-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 本人关系和进行中状态的精确筛选口径是什么？
  > 本人是 creator，或有 JOINED/REVIEWED/SKIPPED 报名；约球必须存储 OPEN 且 end_time>NOW()。
  → 已写入业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 未开始、存储 ONGOING/FULL 和创建者无报名如何处理？
  > 不检查 start_time，未开始与已开始未结束都入选；存储 ONGOING/FULL 不入选。创建者分支不依赖报名，因此创建者无报名或已退出仍可入选。
  → 已写入详细流程第 1-2 步、边界情况与实现提示

- [Q3] 编号游标、多取一项和跨页变化如何处理？
  > 按 biz_id 倒序，lastId 后取严格更小编号，LIMIT size+1；仓储截多取项判 hasMore。跨页状态或关系变化可能遗漏/重复，total 不统计。
  → 已写入活动契约、业务动作 A3、详细流程第 3-4 步与边界情况
