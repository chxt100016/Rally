# tournament.comment-pull.flow.list-comments 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 查看资格与赛事编号如何校验，哪些报名状态允许拉取？
  > 必须登录并存在该赛事报名，唯一禁止 WITHDRAWN；其余状态均可。tournamentId 是 @RequestParam 必须出现但无 @NotBlank，空字符串或未知值最终按 TOURNAMENT_ENTRY_NOT_FOUND；不查询赛事状态。
  → 已写入触发、详细流程第 2 步、资格异常与服务边界

- [Q2] 分页条数、游标、排序和空结果的精确契约是什么？
  > limit 缺省20，null也20，小于1取1，大于100取100。beforeCommentId 可选且不校验归属/存在，仅取更早 bizId；结果按 bizId 降序，新到旧。无结果返回空列表，无 next cursor/hasMore。
  → 已写入接口契约、详细流程第 3-4、6 步和分页分支

- [Q3] 何时推进已读位置和重算未读，历史翻页是否会回退？
  > 仅非空批次用第一条（最新）作为候选；无成员记录则创建，已有记录仅在候选 bizId 字符串晚于 lastRead 时推进并按新位置重算未读。空结果或历史页不会建立/修改记录，也不会回退。
  → 已写入详细流程第 5-6 步、流程图与服务边界
