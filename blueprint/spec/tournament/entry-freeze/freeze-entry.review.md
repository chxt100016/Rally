# tournament.entry-freeze.flow.freeze-entry 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权、必填参数以及找不到报名时分别如何处理？
  > POST /tournament/admin/entry/freeze 受后台共享 API Key 保护；TournamentEntryFreezeCmd 的 tournamentId、userId 均 @NotBlank。按二者查询，找不到时报 TOURNAMENT_ENTRY_NOT_FOUND，鉴权或参数失败前不修改报名。
  → 已写入触发、接口契约、详细流程第 1-2 步及鉴权/参数/存在性分支

- [Q2] 允许冻结哪些报名状态，重复冻结是否按幂等成功？
  > TournamentEntry.freeze 仅接受 WAITING；FROZEN 在内的其他所有状态都报 TOURNAMENT_ENTRY_STATUS_ILLEGAL，因此重复冻结不按幂等成功。
  → 已写入详细流程第 3 步与状态异常分支

- [Q3] 冻结会修改哪些字段、是否联动搭档或通知，以及保存失败如何收场？
  > 只把指定用户这一条报名的 status 改为 FROZEN 并保存；不修改阶段、轮次、参赛编号和偏好，不联动同参赛编号搭档，也不发送通知。应用方法有 @Transactional，保存异常时回滚。
  → 已写入详细流程第 4 步、服务边界、保存异常分支及事务线索
