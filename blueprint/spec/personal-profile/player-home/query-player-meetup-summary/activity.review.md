# personal-profile.player-home.activity.query-player-meetup-summary 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 完成约球数采用什么报名与约球状态口径？
  > 目标报名 REVIEWED/SKIPPED 且约球非 DRAFT 即计数，不检查结束时间或其他终态。
  → 已写入业务动作 A1 与详细流程第 1 步

- [Q2] 最近约球的成员范围、状态、排序和数量是什么？
  > 目标为创建者，或报名 JOINED/REVIEWED/SKIPPED；排除 DRAFT，其余状态保留，按 bizId 倒序，底层请求4条而产品交付最多3张。
  → 已写入业务动作 A2、详细流程第 2、5 步与实现提示

- [Q3] 最近卡片的状态标签和背景如何组装？
  > RECENT 标签用有效状态文案，OPEN 且已过 endTime 显示 FINISHED；有 courtId 时取材质和室内外结合时段解析背景，球场不存在按未知降级。
  → 已写入业务动作 A3、详细流程第 3-4 步与边界情况
