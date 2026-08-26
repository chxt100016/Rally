# meetup.meetup-finish-settlement.flow.settle-finished-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 约球结束结算在什么开关和调度频率下执行，一次扫描是否分页或限量？
  > 按 Java 实现确认：只有 job.meetup.enabled=true 时注册任务；cron 取 job.meetup.status.cron，默认 0 0 2 * * ?。一次直接执行无分页、无上限的批量 UPDATE，基准时间为构造条件时的 LocalDateTime.now。
  → 已落入触发、接口契约和详细流程。

- [Q2] 批量结算实际识别哪些大小写状态，是否处理 ONGOING 或大写 FULL？
  > 按 Java 实现确认：条件精确写为 OPEN 和小写 full。不会处理 ONGOING、大写 FULL、DRAFT、CLOSED 或已为 FINISHED 的记录，也不区分 NORMAL 与 TOURNAMENT。
  → 已落入详细流程、异常分支和服务边界。

- [Q3] 批量更新失败时是否逐条部分成功、自动重试或向外部返回失败？
  > 按 Java 实现确认：底层是一条批量 UPDATE，不逐条编排；任务层捕获所有异常只记日志，没有外部结果、补偿或本轮自动重试。数据库语句失败时由数据库执行语义决定，仍未结算的记录等下一次调度。
  → 已落入详细流程和异常分支。
