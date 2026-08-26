# pro-tour-data.tournament-draw-collect.flow.collect-tournament-draws-scheduled 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务开关、底层 cron、DRAW 实际频率和时区语义是什么？
  > TourCollectJob 仅 job.tour.enabled=true 时装配；统一 matches() 按 job.tour.collect.live.cron，生产为 0 */5 * * * ?，@Scheduled 无 zone。每次用 Unix epochMinute 判断 Phase.DRAW.shouldRun，即 epochMinute mod 60=0，所以 DRAW 每 60 分钟在绝对整点执行。
  → 已写入触发、详细流程第 1 步及调度技术线索

- [Q2] 定时触发选择哪些赛事并如何按巡回赛与级别路由来源？
  > DRAW 调用 currentDraws，按运行时 LocalDate 选 startDate<=明日且 endDate>=昨日，不看 status。路由为 ATP GS→ATP_APP_DRAW/其他→ATP_DRAW，WTA GS→ATP_APP_DRAW/其他→WTA_DRAW，并为所有 WTA 追加 ATP_APP_COMPLETED。
  → 已写入接口契约、详细流程第 2-4 步与路由调用线索

- [Q3] 单项与阶段级异常如何隔离，是否有业务结果、重试或补偿？
  > currentDraws 内逐赛事 catch RuntimeException，失败项记日志后继续。列表读取等逃出的阶段异常再由 TourCollectJob 对 DRAW 阶段 catch 并记录，不影响同次循环已经先执行的 LIVE/OOP。无业务响应、失败对象记录、即时重试或补偿，只等后续调度。
  → 已写入详细流程第 6 步、异常分支与重试补偿说明

- [Q4] 签表内各对象的保存顺序、覆盖规则与中途失败结果是什么？
  > 每份签表依次保存 draw、matches、players、entries，各自提交；身份键分别为赛事+年+类型、draw+match、tour+player、draw+player。来源非空字段刷新，遗漏不删除，比赛状态不校验方向。后续步骤失败不回滚前序，形成部分快照。
  → 已写入详细流程第 4-5 步、业务活动及部分提交说明
