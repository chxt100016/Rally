# tournament.tournament-withdraw.activity.withdraw-tournament-entry 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名可退赛且重复是否幂等？
  > 除 WITHDRAWN/ELIMINATED 外均可；重复退赛失败，不幂等。
  → 已写入活动契约、异常分支与详细流程第 2 步

- [Q2] 退赛本步骤改变哪些字段？
  > 只改 status=WITHDRAWN，赛段、轮次、偏好、计数和时间保留。
  → 已写入详细流程第 3 步

- [Q3] 是否退款且后续失败会怎样？
  > refundTriggered 固定 false；退出讨论或终止比赛失败会使整个退赛事务回滚。
  → 已写入详细流程第 4 步与边界情况

- [Q4] 退赛拒绝的终态是否包含 CHAMPION？
  > 是。CHAMPION 与 WITHDRAWN、ELIMINATED 一样返回 TOURNAMENT_ENTRY_STATUS_ILLEGAL。
  → 活动触发条件、异常分支与详细流程已补齐 CHAMPION。
