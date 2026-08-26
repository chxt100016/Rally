# tournament.result-confirmation-timeout.flow.complete-timeout-results 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务启用条件、调度表达式、超时阈值和扫描时间字段是什么？
  > 仅 job.tournamentMatchTimeout.enabled=true 时加载；共享调度 ${job.tournamentMatchTimeout.cron:0 0 */2 * * ?}，默认每两小时。结果分支以 now-48h 为阈值，查询 status=PENDING_CONFIRM 且 submittedTime<=阈值；submittedTime 为 null 不命中。
  → 已写入触发、接口契约、详细流程第 1 步及 Job 技术线索

- [Q2] 自动确认会处理哪些参与者状态，是否会因已有 REJECTED 而拒绝完成？
  > 逐场再次检查状态；非 PENDING_CONFIRM 直接返回。只把 participant.resultConfirmStatus=PENDING 改为 CONFIRMED 并写同一 now，CONFIRMED/REJECTED 均原样保留；即使保留 REJECTED，仍直接把比赛置 COMPLETED。
  → 已写入详细流程第 2-3 步与幂等分支

- [Q3] 报名结算、轮次推进、并发及单场失败对整批的影响是什么？
  > 按 winnerEntryNo 结算胜负报名并调用 advanceIfReady；不先校验报名原状态。每场方法独立事务且版本更新防并发；job 对每场 try/catch，单场失败回滚并记日志，不阻断同批其他场。
  → 已写入详细流程第 4-6 步、逐场异常和事务边界
