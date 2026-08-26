# tournament.tournament-abandon.activity.abandon-tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些状态可废弃且重复是否幂等？
  > DRAFT/ACTIVE 可转 ABANDONED；已 ABANDONED 重复调用失败。
  → 已写入活动契约、异常分支与详细流程第 1 步

- [Q2] reason 是否保存？
  > 当前不传入领域、不保存，即使请求提供也丢弃。
  → 已写入活动契约、详细流程第 2 步与边界情况

- [Q3] 废弃会联动哪些关联对象？
  > 不会联动报名、比赛、赛约、支付、退款、线下活动或通知，只改赛事状态。
  → 已写入详细流程第 3-4 步与边界情况
