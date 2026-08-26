# tournament.entry-unfreeze.flow.unfreeze-entry 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 解冻如何限定为本人操作，入口与用户资料需要哪些条件？
  > UserContext 识别当前用户，只按 tournamentId 查本人报名；赛事编号 @NotBlank。还会读取本人 UserProfile，要求 user 存在且 phone 非空，否则 USER_PHONE_REQUIRED。
  → 已写入触发、接口契约、详细流程第 1、3-4 步及身份异常

- [Q2] 赛事状态、结束时刻和报名状态的精确允许范围是什么？
  > TournamentPolicy 要求赛事严格为 ACTIVE；endTime 为空可通过，now 恰等于 endTime 也通过，只有 now.isAfter(endTime) 才拒绝。报名必须严格为 FROZEN，重复解冻不幂等。
  → 已写入详细流程第 2、4 步与状态异常分支

- [Q3] 成功解冻修改哪些字段，是否联动搭档、比赛或立即匹配？
  > 只把本人这一条报名 status 从 FROZEN 改为 WAITING 并事务保存；阶段、轮次、entryNo、partnerId、偏好不变，不联动搭档，不检查或关闭比赛，也不立即执行匹配或通知。
  → 已写入详细流程第 5 步与服务边界
