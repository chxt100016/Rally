# tournament.tournament-activate.flow.activate-tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 后台鉴权、赛事编号与不存在时如何处理？
  > POST /tournament/admin/activate 受 AdminApiKeyInterceptor。TournamentActivateCmd.tournamentId @NotBlank；查无走 TOURNAMENT_NOT_FOUND。
  → 已写入接口契约、详细流程第 1 步及鉴权/存在性分支

- [Q2] 哪些状态可激活，所谓配置完整具体校验哪些时间？
  > Tournament.assertCanActivate 仅允许 DRAFT。配置完整只要求 registrationStartTime 与 qualifierStartTime 非null，并要求前者 isBefore 后者；不检查其他创建字段、截止时间、当前时间。
  → 已写入详细流程第 2-3 步与状态/时间分支

- [Q3] 激活保存、事务、联动与成功响应是什么？
  > 只 set status=ACTIVE 后 repository.save；应用方法 @Transactional，失败回滚。其他配置/进度不变，不创建报名或任务。成功 Result.ok() data=null。
  → 已写入详细流程第 4 步、服务边界与事务线索
