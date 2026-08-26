# court.court-activity-heat-settlement.activity.settle-court-heat 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 昨日时间窗、状态和选场模式的精确口径是什么？
  > 以运行环境 LocalDate 昨日的 LocalTime.MIN 到 LocalTime.MAX 为闭区间；仓储只返回结束时间范围内且状态 FINISHED 的约球；活动再只保留 courtId 非空且 courtSelectMode 为 MAP 或 TEXT 的记录。
  → 已写入业务动作 A1-A2、详细流程第 1-2 步与边界情况

- [Q2] 如何分组累加，无匹配球场和空 meetup_count 怎么办？
  > 按 courtId 计数，每个球场执行 meetup_count=COALESCE(meetup_count,0)+count；biz_id 不存在时影响 0 行且当前不单独报错。无合格记录直接结束。
  → 已写入领域依赖、业务动作 A3-A4、详细流程第 3-4 步与边界情况

- [Q3] 重复调度、部分更新失败和事务补偿如何处理？
  > 无结算幂等标记，同一昨日窗口重复执行会重复累加。各球场逐个更新，不设覆盖整批的总事务；任一异常终止并由 job 记录，已更新球场不回滚、不自动重试。
  → 已写入异常分支、详细流程第 5 步与边界情况
