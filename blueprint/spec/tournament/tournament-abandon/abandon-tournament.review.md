# tournament.tournament-abandon.flow.abandon-tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 后台鉴权、赛事编号、原因与不存在时如何处理？
  > POST /tournament/admin/abandon 受后台 key。TournamentAbandonCmd.tournamentId @NotBlank，reason 可选；查无赛事不存在。reason 未传入领域方法，完全忽略。
  → 已写入接口契约、详细流程第 1-2 步及鉴权/存在性分支

- [Q2] 哪些状态可废弃，重复废弃如何处理？
  > Tournament.assertCanAbandon 允许任何非 ABANDONED 状态，当前枚举即 DRAFT/ACTIVE；ABANDONED 重复请求报状态不允许，不幂等。
  → 已写入详细流程第 2 步与重复废弃分支

- [Q3] 废弃保存、事务、关联对象与成功响应是什么？
  > 只 set status=ABANDONED 并在 @Transactional 应用方法保存；失败回滚。endTime、配置和所有关联对象不变，无退款/通知/审计。成功 Result.ok() data=null。
  → 已写入详细流程第 3-4 步、服务边界与事务线索
