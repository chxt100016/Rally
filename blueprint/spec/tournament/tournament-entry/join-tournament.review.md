# tournament.tournament-entry.flow.join-tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 用户资料、赛事状态、报名窗口、性别、NTRP 和手机号分别按什么口径校验？
  > 当前用户必须能读取账户，昵称/头像不能仍为默认且网球档案必须存在并非 TBC；手机号必须已绑定。赛事必须 ACTIVE，当前时间须不早于报名开始且不晚于非空截止时间；性别按赛事限制判断，NTRP 按十进制数值相等判断，缺失等级不通过。
  → 已写入详细流程第 2-3 步、准入异常与技术线索

- [Q2] partnerId 在哪些赛事类型可填，搭档未报名、已报名未配对或已与他人配对时如何分配参赛编号？
  > 现实现不按 SINGLE/DOUBLE/RALLY 限制 partnerId，也不校验搭档用户存在、是否本人或搭档资格。无 partnerId 或搭档无报名时分配新编号；搭档报名未配对或已配本人时复用其编号，未配对时补 partnerId=userId；已配其他人时报 TOURNAMENT_PARTNER_ALREADY_PAIRED。
  → 已写入详细流程第 4 步、搭档异常与服务边界

- [Q3] 报名、搭档反向关系和赛事讨论成员是否同一事务，已有孤立讨论成员记录时如何收场？
  > TournamentEntryAppService.join 以同一事务包含搭档反向关系、本人报名和讨论成员创建；任一步失败整体回滚。若已有聊天成员但无报名，加入讨论报 ALREADY_JOINED_CHAT，新报名和本次搭档关系不保留，既有孤立成员记录保持。
  → 已写入详细流程第 5-6 步、流程图与事务异常

- [Q4] 初始报名状态和容量口径是什么，微信通知场景如何过滤且授权登记失败是否影响报名？
  > 报名初始固定 QUALIFY/WAITING/QUALIFIER，两类拒赛次数为 0；不检查 totalSlots/currentFilledSlots，也不占正赛席位。只接受并去重 TOURNAMENT_MATCHED、TOURNAMENT_BOOKING_SUBMITTED、TOURNAMENT_REJECTED，其他值忽略；授权保存内部捕获异常，不向用户报错且报名继续成功。
  → 已写入详细流程第 5、7 步、通知异常与服务边界
