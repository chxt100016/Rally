# pro-tour-data.tournament-schedule-collect.flow.collect-match-schedules-scheduled 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务开关、底层 cron、OOP 实际频率和时区是什么？
  > TourCollectJob 仅 job.tour.enabled=true 装配；matches() 使用生产 0 */5 * * * ? 且无 zone。Phase.OOP interval=60，通过 epochMinute mod 60=0 执行，所以实际每小时绝对整点触发。
  → 已写入触发、详细流程第 1 步及调度技术线索

- [Q2] 定时目标与四类来源路由、单打范围是什么？
  > 目标为本地日期 startDate<=明日、endDate>=昨日，不筛 status。普通 ATP→ATP_OOP，ATP GS→ATP_SCHEDULE，普通 WTA→WTA_SCHEDULE，WTA GS→ATP_SCHEDULE_FOR_WTA；未知 tour 跳过、空 category 异常；默认只单打。
  → 已写入接口契约、详细流程第 2-3 步与路由线索

- [Q3] 赛程字段、随后推算及非空覆盖规则是什么？
  > 保存比赛日期、转为 Asia/Shanghai 的计划时间、来源文本、场地场序、轮次对阵和可能附带的状态/胜方/结束/比分；followed-by 对 ATP_OOP 加100分钟，其他 schedule 加70分钟。解析失败为 null，存量只非空/非blank覆盖，状态可回退；来源遗漏不删。
  → 已写入详细流程第 3-4 步、异常补充与时区线索

- [Q4] 异常是否中止后续赛事和同轮 DRAW，是否有重试、补偿或结果？
  > oop() 无逐赛事 catch，任一未处理异常中止后续赛事并冒泡；TourCollectJob 捕获 OOP 阶段异常，然后循环仍进入同轮 DRAW。已提交赛事/步骤保留。无业务结果、即时重试或补偿，等待下次 OOP 门槛。
  → 已写入详细流程第 5-6 步、异常分支及阶段顺序说明
