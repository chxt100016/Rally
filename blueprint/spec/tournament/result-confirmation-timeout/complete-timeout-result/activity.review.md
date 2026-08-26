# tournament.result-confirmation-timeout.activity.complete-timeout-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 超时阈值和幂等复核是什么？
  > 固定提交后 48 小时；逐场重载，已非 PENDING_CONFIRM 则跳过。
  → 已写入触发条件、活动契约与详细流程第 1 步

- [Q2] 自动确认会覆盖哪些参与者状态？
  > 只把 PENDING 改 CONFIRMED 并写统一时间，其他 CONFIRMED/REJECTED 等保持。
  → 已写入业务动作 A2、详细流程第 2 步与边界情况

- [Q3] 单场缺胜方或保存失败会影响同批吗？
  > 该场事务回滚并记录，外层继续其他场；已成功场次保留。
  → 已写入异常分支、详细流程第 4-5 步与边界情况
