# tournament.court-booker-selection-timeout.flow.reject-timeout-matches 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务开关、调度表达式、超时阈值和扫描字段是什么？
  > job.tournamentMatchTimeout.enabled=true 时加载，调度 ${job.tournamentMatchTimeout.cron:0 0 */2 * * ?} 默认每两小时；MATCHED 分支以 now-3days 查询 matchedTime<=阈值。
  → 已写入触发、接口契约、详细流程第 1 步与 Job 线索

- [Q2] 超时终止精确修改比赛、赛约和报名哪些字段，哪些异常状态保留？
  > 比赛只写 status=REJECTED、rejectReasonCode=TIMEOUT，不写拒绝人/阶段/时间/完成时间；只关闭存在且为 DRAFT 的赛约；只将 IN_MATCH 报名改 WAITING，其他状态和所有轮次/计数保持。
  → 已写入详细流程第 3-4 步、容错分支和服务边界

- [Q3] 逐场幂等、并发冲突与单场失败对批次的影响是什么？
  > 逐场重载后非 MATCHED 直接返回；版本更新防并发，冲突或缺报名等使该场事务回滚。Job 每场 try/catch，记录失败后继续其他场，无整批事务。
  → 已写入详细流程第 2、5 步、并发异常与事务边界
