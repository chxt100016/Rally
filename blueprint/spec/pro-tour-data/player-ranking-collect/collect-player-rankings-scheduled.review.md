# pro-tour-data.player-ranking-collect.flow.collect-player-rankings-scheduled 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 定时任务的装配开关、cron、时区和运行环境门槛是什么？
  > TourCollectJob 仅在 job.tour.enabled=true 时装配；rank 使用配置 cron 0 0 4 * * ?，@Scheduled 未声明 zone，按 JVM 默认时区。触发后 active profiles 必须包含精确值 wechat，否则直接 no-op。
  → 已写入触发、接口契约、详细流程第 1-2 步与调度技术线索

- [Q2] ATP/WTA 来源为空与来源抛出异常分别如何继续或终止？
  > ATP 先于 WTA。任一来源 response/data/rankings/players 为空时该巡回赛只记警告并跳过，任务继续；客户端或后续未捕获异常向调度框架抛出。ATP 抛错会阻止 WTA，WTA 抛错则在 ATP 已处理后终止。
  → 已写入详细流程第 3/7 步、流程图及来源空/异常分支

- [Q3] 球员身份、本批去重、非空覆盖以及来源遗漏记录的规则是什么？
  > 以 tour+playerId 严格识别；保存前过滤两者任一为空的记录，并在批内按同键去重，后到记录仅以非空字段合并。存量同键也只覆盖非空字段；本次未出现的存量球员不删除、不降级、不清空旧排名。
  → 已写入详细流程第 4-5 步、异常补充说明与服务边界

- [Q4] ATP 与 WTA 的事务边界及部分成功结果是什么？
  > ATP 与 WTA 各由一次独立的事务性 saveOrUpdateBatch 保存。单批失败回滚该批；ATP 已提交后 WTA 失败仍保留 ATP，因此当日快照可能部分更新，没有跨批补偿。
  → 已写入详细流程第 6-7 步及独立事务异常分支

- [Q5] 任务完成或失败后是否产生业务提示、统计、补偿或重试？
  > 非 wechat 环境和来源为空均静默结束或跳过；成功也不输出业务结果、计数或审计记录。方法没有 catch、业务重试或补偿，未处理异常交给 Spring 调度器记录；下一次只依赖后续 cron 触发。
  → 已写入成功响应、详细流程第 8 步、异常分支与技术线索
