# meetup.meetup-finish-settlement.activity.settle-finished-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 调度开关、时间基准和扫描范围是什么？
  > 任务 bean 仅在 job.meetup.enabled=true 时注册，cron 默认每日02:00；每次在构造 UPDATE 时取 LocalDateTime.now()，扫描全部符合条件记录，不分页、不先冻结清单。
  → 已写入触发条件、业务动作 A1-A2 与详细流程第 1 步

- [Q2] 状态匹配的大小写和时间边界是什么？
  > SQL 条件 status IN ('OPEN','full') 且 end_time < 当前时间，目标写 'FINISHED'。不匹配 ONGOING、大写 FULL 或其他状态；end_time 等于当前基准不命中。普通和赛事约球不区分。
  → 已写入领域依赖、业务动作 A2-A3、详细流程第 2-4 步与边界情况

- [Q3] 批量原子性、幂等、影响行数与失败重试如何处理？
  > 单条批量 UPDATE，数据库语句内原子；已转 FINISHED 后不会再次命中，重复调度幂等。返回影响行数用于日志。异常由 job 捕获记录，本轮不重试、不做逐对象统计，仍符合条件的留待下次。
  → 已写入活动契约、详细流程第 4-5 步与边界情况
