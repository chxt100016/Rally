# tournament.booking-submit.activity.save-booking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 新建和更新赛约分别校验什么？
  > 新建要求 BOOKING 且本人为订场人；更新要求当前关联赛约存在、本人为创建人且比赛为 BOOKING/SCHEDULED。
  → 已写入活动契约与详细流程第 2-3 步

- [Q2] BOOKING 与 SCHEDULED 提交对确认状态有何不同？
  > BOOKING 推进 SCHEDULED，订场人确认、其他人待确认；SCHEDULED 内修改保留已有确认状态。
  → 已写入业务动作 A4、详细流程第 4 步与边界情况

- [Q3] 球场、并发和通知如何降级？
  > courtId 未命中用请求资料；仅 BOOKING 推进比较版本；通知提交后容错且不影响成功。
  → 已写入详细流程第 1、5-6 步与边界情况
