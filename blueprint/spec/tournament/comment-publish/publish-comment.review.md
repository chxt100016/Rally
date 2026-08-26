# tournament.comment-publish.flow.publish-comment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 评论发布资格如何由赛事报名状态判定，是否校验赛事本身？
  > 当前用户必须存在该赛事报名，且唯一禁止 WITHDRAWN；WAITING/FROZEN/IN_MATCH/PAYING/ELIMINATED 均可。不查询赛事，不校验其存在、状态或开放期。
  → 已写入触发、详细流程第 2 步、资格异常和服务边界

- [Q2] 内容、发布者快照和重复提交如何处理？
  > tournamentId/content @NotBlank，contentType @NotNull 且仅 TEXT/IMAGE/LOCATION。保存发布时 UserProfile 的昵称和头像快照；无幂等键，每次重复请求都创建新评论。
  → 已写入接口契约、详细流程第 1、3、5 步和服务边界

- [Q3] 发布后成员已读/未读如何变化，部分失败如何收场？
  > 其他已有成员未读+1；发布者 lastRead 推到新评论、unread=0，缺关系时补建，因此之前未读也一并视为已读。评论、成员变化和 DTO 头像签名都在应用事务中，异常整体回滚。
  → 已写入详细流程第 4-5 步、保存异常与事务线索
