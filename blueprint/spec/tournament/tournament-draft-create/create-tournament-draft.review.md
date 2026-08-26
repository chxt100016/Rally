# tournament.tournament-draft-create.flow.create-tournament-draft 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 后台鉴权、请求字段校验与枚举/城市失败如何处理？
  > POST /tournament/admin/create 受 AdminApiKeyInterceptor 保护而排除普通登录鉴权；共享 key 缺失/不匹配返回无权限。TournamentCreateCmd Bean Validation 校验必填、长度、颜色、最小/非负与奖金格式。字符串枚举 valueOf 失败、城市编码 lookup 无结果解引用均走 OPERATION_FAILED。
  → 已写入接口契约、详细流程第 1/3-4 步及鉴权/参数异常分支

- [Q2] 签位、线下轮次、费用、奖金和时间的业务约束是什么？
  > totalSlots 必须 2/4/8/16/32/64；offlineRound 对应 slots 必须严格小于 totalSlots。qualifierGroupSize>=2，entryFee/两个 rejectLimit>=0，奖金是一个或多个逗号分隔数字。registrationStart < qualifierStart；registrationEnd>=registrationStart；qualifierEnd>=qualifierStart，未约束 registrationEnd 与 qualifierStart。
  → 已写入详细流程第 2-3 步与业务规则异常分支

- [Q3] 初始状态、默认字段、事务边界和成功响应是什么？
  > 生成业务编号并保存配置，强制 status=DRAFT、currentRound=QUALIFIER、mainDrawLockedSlots=0、endTime/offlineMeetupId=null。TournamentAdminAppService.create @Transactional，任何异常回滚。成功返回 Result<TournamentIdDTO> 只含 tournamentId，不创建其他对象。
  → 已写入详细流程第 5-6 步、接口契约与事务线索
