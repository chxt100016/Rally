# platform-config.home-content-query.activity.query-home-meetup-section 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 匿名和可选鉴权失败时约球区如何返回？
  > 都按匿名处理，保留区域文案并返回空 meetups，不查询约球。
  → 已写入活动契约、业务动作 A2、详细流程第 1-2 步与边界情况

- [Q2] 登录用户的进行中约球筛选与数量是什么？
  > 报名 JOINED/REVIEWED/SKIPPED，约球 OPEN 且 endTime 未到，按 bizId 倒序取默认10条，不返回游标。
  → 已写入业务动作 A2-A3 与详细流程第 3-4 步

- [Q3] 区域构建失败如何影响首页？
  > 约球、报名、球场或卡片转换异常只省略 MEETUP 区域，其他已形成和后续区域继续。
  → 已写入异常分支与边界情况
