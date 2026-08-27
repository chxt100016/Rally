# tournament.booking-confirm.activity.confirm-booking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 重复确认与异常原确认状态如何处理？
  > 本人无论原为 PENDING、CONFIRMED 或异常 REJECTED 均覆盖为 CONFIRMED 并刷新确认时间。
  → 已写入活动契约、详细流程第 2 步与边界情况

- [Q2] 何时推进比赛并开放赛约？
  > 尚有未确认者保持 SCHEDULED；全员确认进入 PENDING_PLAY，且仅关联赛约存在并为 DRAFT 时改 OPEN。
  → 已写入业务动作 A3-A4 与详细流程第 3-4 步

- [Q3] 赛约缺失或并发失败如何收敛？
  > 赛约空/缺失/非 DRAFT 不阻止推进；比赛版本冲突或保存失败使事务整体回滚。
  → 已写入异常分支、详细流程第 4-5 步与边界情况

- [Q4] 关联 meetupId 为空或赛约记录缺失时是否继续跳过过期校验并允许确认？
  > 是。关联缺失保持现有兼容行为，只在记录存在时执行过期校验。
  → 已落入领域依赖、业务动作 A2 与详细流程第 2 步。

- [Q5] 本人原状态为 CONFIRMED 或 REJECTED 时是否继续覆盖为 CONFIRMED 并刷新时间？
  > 是。保持覆盖确认语义，不新增 PENDING 前置条件。
  → 已落入业务动作 A3、详细流程第 3 步与边界情况。

- [Q6] 全员确认时关联赛约不存在或非 DRAFT 是否继续不阻止比赛进入 PENDING_PLAY？
  > 是。比赛照常推进，只对存在且为 DRAFT 的赛约执行开放。
  → 已落入活动契约、业务动作 A5 与详细流程第 5 步。

- [Q7] 关联赛约开始时间恰等于本次确认时间时，是否应按过期拒绝确认？
  > 否。保持 main 与 @tournament.match：仅 startTime 严格早于 confirmTime 时过期；两者恰等时继续确认。
  → flow、service、activity 已统一为严格早于才过期，恰等继续。
