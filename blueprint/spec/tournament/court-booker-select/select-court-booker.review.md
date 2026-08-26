# tournament.court-booker-select.flow.select-court-booker 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 谁能认领订场，是否检查报名订场能力？
  > 当前登录用户必须出现在比赛参与者列表；不查询赛事报名，也不检查 courtAbility，因此任何参与者都可认领。
  → 已写入触发、详细流程第 3 步和服务边界

- [Q2] 允许状态和重复认领口径是什么，成功写哪些字段？
  > 比赛必须严格 MATCHED，任何其他状态都报 TOURNAMENT_COURT_BOOKER_ALREADY_SELECTED，重复请求不幂等。成功写 courtBookerId、courtBookerSelectedTime，并将 status 改为 BOOKING。
  → 已写入详细流程第 2、4 步及状态异常

- [Q3] 并发抢领和保存失败如何决定最终订场人，是否创建赛约或通知？
  > updateWithVersion 防并发；只有先成功更新者生效，后请求报版本冲突且事务回滚。本服务不创建/更新赛约，不修改报名或参与关系，也不发送通知。
  → 已写入并发/保存异常、技术线索与服务边界
