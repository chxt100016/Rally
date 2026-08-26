# tournament.court-booker-selection-timeout.activity.reject-timeout-match 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 候选阈值和逐场复核规则是什么？
  > 扫描 MATCHED 且 matchedTime≤当前减三天；逐场重载后已非 MATCHED 则幂等跳过。
  → 已写入触发条件、活动契约与详细流程第 1 步

- [Q2] 赛约和报名分别何时变化？
  > 只关闭 DRAFT 赛约，只把 IN_MATCH 报名改 WAITING；其他状态保持。
  → 已写入业务动作 A3-A4、详细流程第 3-4 步与边界情况

- [Q3] 单场失败会否阻断批次？
  > 不会；每场独立事务，外层按场捕获并继续，失败候选可在后续轮次重试。
  → 已写入异常分支、详细流程第 5 步与边界情况
