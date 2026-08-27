# tournament.tournament-withdraw.flow.withdraw-tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名状态允许退赛，是否检查赛事状态、时间、阶段或本人是否已支付？
  > 只禁止 WITHDRAWN 和 ELIMINATED；WAITING、FROZEN、IN_MATCH、PAYING 以及其他非终态均可。只要求当前用户在指定赛事有报名，不读取赛事本身，也不检查赛事状态、结束时间、报名阶段、退赛截止或是否支付。
  → 已写入详细流程第 1-2 步、状态异常与服务边界

- [Q2] 本人报名、讨论成员、比赛、赛约和其他报名是否在同一事务，讨论成员不存在时能否继续？
  > TournamentEntryAppService.withdraw 的 @Transactional 覆盖本人报名保存、讨论成员删除、比赛版本更新、赛约和其他报名保存；任一步抛错整体回滚。ChatDomainService.quit 直接按关系删除，成员不存在也可继续，不删除历史评论。
  → 已写入详细流程第 2-6 步、持久化异常与技术线索

- [Q3] 在途比赛如何选择和终止，关联赛约缺失或非 DRAFT、同场报名非 IN_MATCH 时如何处理？
  > 按赛事和用户查询状态不为 COMPLETED/REJECTED 的一场比赛，异常多场时无明确排序且只处理一场。版本更新为 REJECTED；meetupId 为空、活动不存在或非 DRAFT 均跳过，只有 DRAFT 改 CLOSED；仅把参与者中状态仍为 IN_MATCH 的报名改 WAITING。
  → 已写入详细流程第 4-6 步、分支异常与技术线索

- [Q4] 双打搭档、正赛席位、支付退款、拒赛次数和通知在退赛时如何处理？
  > 只把发起人置 WITHDRAWN，不直接修改搭档关系或搭档报名；若搭档在被终止比赛中且仍为 IN_MATCH，会同其他人一起回 WAITING。不回收正赛席位、不关闭支付单、不退款，返回 refundTriggered=false；不累计拒赛次数，也不发送通知。
  → 已写入详细流程第 7 步、服务边界与接口契约

- [Q5] CHAMPION 报名是否允许退赛？
  > 否。保持 main 与 @tournament.entry C9：CHAMPION、WITHDRAWN、ELIMINATED 三种终态均禁止退赛。
  → flow、service 已补齐 CHAMPION 终态限制。
