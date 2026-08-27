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

- [Q4] SCHEDULED 内修改是否继续保留确认状态、跳过比赛版本校验且不重复通知？
  > 是。SCHEDULED 内仅修改当前赛约资料，保留所有参与者原确认状态与确认时间，不比较比赛版本，也不重复发送订场通知。
  → 已落实到业务动作 A4、详细流程第 5/7 步与边界情况。

- [Q5] BOOKING 且未传 meetupId 时是否继续允许新建赛约并覆盖当前关联？
  > 是。BOOKING 且未传 meetupId 时继续允许订场人新建 DRAFT 赛约，并以新赛约编号写入比赛当前关联；不额外拒绝已有旧关联的情况。
  → 已落实到活动契约、业务动作 A3 与详细流程第 3 步。

- [Q6] 订场通知是否继续使用 matchId 与 scheduleSubmittedTime 构造稳定事件并在提交后容错发送？
  > 是。仅 BOOKING 首次成功提交后，以 matchId 与 scheduleSubmittedTime 构造稳定事件，事务提交后向其他参与者尽力发送；失败不回滚且不自动重试。
  → 已落实到 @notification.delivery 领域依赖、业务动作 A5 与详细流程第 7 步。
