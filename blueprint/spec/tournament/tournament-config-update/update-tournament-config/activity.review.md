# tournament.tournament-config-update.activity.update-tournament-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些赛事状态允许更新？
  > DRAFT、ACTIVE、ABANDONED 均允许，不设状态限制。
  → 已写入触发条件、活动契约与边界情况

- [Q2] 可选字段传 null 是否清除旧值？
  > 不能；映射后实体更新忽略 null，数据库旧值保留。
  → 已写入活动契约、详细流程第 3 步与边界情况

- [Q3] 城市和运营进度如何处理？
  > cityCode 可变但 cityName 不重算；状态、轮次、锁位、结束时间和线下关联保留。
  → 已写入详细流程第 4-5 步与边界情况
