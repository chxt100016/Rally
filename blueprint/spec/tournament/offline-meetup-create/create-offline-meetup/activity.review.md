# tournament.offline-meetup-create.activity.create-offline-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时允许创建且如何保证唯一？
  > 赛事 currentRound 必须等于 offlineRound 且未绑定；最后以关联为空条件更新，并发只保留先绑定结果。
  → 已写入触发条件、异常分支与详细流程第 1、5 步

- [Q2] 候选成员如何选择？
  > 仅该轮 WAITING 报名，按 userId 去重且必须非空；全部直接以 JOINED 加入。
  → 已写入活动契约、业务动作 A2-A4 与详细流程第 2、4 步

- [Q3] 活动等级和失败事务如何处理？
  > 赛事 NTRP 转精确等级并校验约球规则；活动、成员和赛事绑定同事务，任一失败整体回滚。
  → 已写入异常分支、详细流程第 3-5 步
