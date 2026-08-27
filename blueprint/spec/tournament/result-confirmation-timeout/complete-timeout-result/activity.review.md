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

- [Q4] 超时任务是否继续固定使用 48 小时阈值，并只以 submittedTime 不晚于截止时间筛选，不新增可配置项？
  > 是。继续固定以当前时间减 48 小时作为截止点，并按 submittedTime 筛选，不新增配置或改变调度契约。
  → 已落实到触发条件、活动契约、详细流程第 1 步与实现提示。

- [Q5] 逐场处理时是否仅把 PENDING 确认改为 CONFIRMED，保留已经 CONFIRMED 或 REJECTED 等其他状态？
  > 是。只补齐 PENDING 为 CONFIRMED 并使用同一处理时间；已经是 CONFIRMED、REJECTED 等状态保持原值。
  → 已落实到 @tournament.match、业务动作 A2、详细流程第 2 步与边界情况。

- [Q6] 某场重载后状态已变化是否静默跳过，其他校验或持久化失败则仅回滚该场并由外层继续后续候选？
  > 是。重载后非 PENDING_CONFIRM 静默跳过；其余失败只回滚本场，由外层逐场捕获并继续，已成功场次保留。
  → 已落实到异常分支、业务动作 A1/A4、详细流程第 1/5 步与边界情况。
