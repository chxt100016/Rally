# pro-tour-data.tournament-live-collect.flow.collect-live-matches-scheduled 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务开关、cron、LIVE 阶段频率和时区是什么？
  > TourCollectJob 仅 job.tour.enabled=true 时装配；matches() 使用 job.tour.collect.live.cron，生产为 0 */5 * * * ?，@Scheduled 无 zone。Phase.LIVE interval=5，每次底层 cron 触发都满足并先执行。
  → 已写入触发、详细流程第 1 步及调度技术线索

- [Q2] 定时任务如何选择赛事、过滤单打并处理空或错赛来源？
  > 按本地日期选择 startDate<=明日且 endDate>=昨日、不看 status。每项调用 ATP_APP_LIVE，ATP 保留 MS、WTA 保留 LS，其他 tour 抛错。空响应/列表/过滤结果或 eventId/year 不符时本项无更新并继续。
  → 已写入接口契约、详细流程第 2-3 步及空来源分支

- [Q3] 状态、比分、签表和比赛的覆盖与事务规则是什么？
  > 状态与盘分按手动流程映射。先独立事务关联/新建 draw，再独立事务按 drawId+matchId 批量 upsert；存量只非空覆盖，状态可回退，来源遗漏不删。比赛失败可留下签表，此前赛事不回滚。
  → 已写入详细流程第 4-5 步、业务活动与覆盖补充说明

- [Q4] 单项异常是否继续、阶段如何记录失败，是否重试或补偿？
  > liveMatch 没有逐赛事 catch，任何转换/保存 RuntimeException 会中止后续赛事并冒泡；TourCollectJob 捕获并记录 LIVE 阶段失败，然后仍可继续同次 OOP/DRAW 阶段循环。无业务响应、即时重试或补偿，下次 LIVE 尝试在后续五分钟触发。
  → 已写入详细流程第 5-6 步、异常分支及阶段调用线索
