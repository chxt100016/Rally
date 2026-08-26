# tournament.entry-preference-update.flow.update-entry-preference 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁可以修改哪一条报名，入口字段有哪些最低校验？
  > UserContext 当前用户只能按 tournamentId 修改本人报名。tournamentId @NotBlank；preferredDistricts、availableTimes @NotEmpty；courtAbility @NotNull 且枚举仅 CAN_BOOK/CANNOT_BOOK。
  → 已写入触发、接口契约、详细流程第 1-2 步及入口异常

- [Q2] 哪些报名状态允许修改，是否与应用层注释所称 WAITING/PAYING 一致？
  > 实际 TournamentEntry.assertCanUpdatePreference 只禁止 ELIMINATED、WITHDRAWN，因此 WAITING、PAYING、FROZEN、IN_MATCH 都允许；这比应用层注释所称等待/待支付更宽，应按运行代码记录。
  → 已写入详细流程第 3 步、状态异常与服务边界

- [Q3] 偏好是增量还是整体替换，是否联动搭档、比赛或赛事状态？
  > 三项字段均按请求整组覆盖；只保存本人报名，不联动搭档，不重排既有比赛，不改变报名状态/轮次/阶段，也不查询或校验赛事状态。
  → 已写入详细流程第 4 步和服务边界
