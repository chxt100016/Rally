# tournament.tournament-config-update.flow.update-tournament-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 鉴权、赛事存在性、允许修改状态和请求校验是什么？
  > POST /tournament/admin/update 由 AdminApiKeyInterceptor 校验共享 key。TournamentUpdateCmd 要求 tournamentId 并继承 TournamentCreateCmd 全部 Bean Validation。按 bizId 查无返回赛事不存在。领域方法不检查状态，DRAFT/ACTIVE/ABANDONED 都可改；跨字段政策与创建相同。
  → 已写入触发、接口契约、详细流程第 1-2 步及鉴权/存在性分支

- [Q2] 哪些配置被覆盖，空值与城市编码/名称如何处理？
  > MapStruct update 覆盖命令提供的名称、图片/主题、类型、cityCode、NTRP、性别、签位、offlineRound、组大小、费用奖金、时间、拒赛限制和规则。null 可选字段按 NullValuePropertyMappingStrategy.IGNORE（需以实际 mapper 为准）保留旧值；update 不查 locationCatalog，cityName 保持旧值，即可能与新 cityCode 不一致。
  → 已写入详细流程第 3-4 步及实体空列/城市名补充说明

- [Q3] 哪些运营字段保留，事务和成功响应是什么？
  > 不映射/保留 bizId、status、currentRound、mainDrawLockedSlots、endTime、offlineMeetupId、create/update审计等进度，也不更新关联对象。TournamentAdminAppService.update @Transactional，校验/保存异常回滚；成功 Result.ok() data=null。
  → 已写入详细流程第 5-6 步、服务边界与事务异常分支
