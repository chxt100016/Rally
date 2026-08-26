# tournament.booking-submit.flow.submit-booking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 首次新建与携带 meetupId 更新分别要求哪些比赛、订场人和创建人条件？
  > 无 meetupId：比赛必须 BOOKING 且当前用户等于 courtBookerId，新建 DRAFT 赛约并全员 JOINED。带 meetupId：赛约须存在、等于 match.meetupId、当前用户为 creator；比赛仅可 BOOKING 或 SCHEDULED；BOOKING 分支还通过 submitBooking 再校验当前用户是订场人。
  → 已写入详细流程第 3-4 步、身份/关联异常和服务边界

- [Q2] BOOKING 提交与 SCHEDULED 内修改对比赛及参与者确认状态有何差异？
  > BOOKING 提交会写 scheduleSubmittedTime、status=SCHEDULED，提交人 CONFIRMED/写时间，其他人 PENDING/清时间。SCHEDULED 内只更新赛约资料，不版本更新比赛，也不重置任何确认状态或时间。赛约始终保持原状态，首次为 DRAFT。
  → 已写入详细流程第 5 步、流程图和服务边界

- [Q3] 请求 tournamentId、球场引用、事务并发和通知失败的实际语义是什么？
  > cmd.tournamentId 虽必填但业务忽略，赛事取 match.tournamentId。TEXT/MAP courtId 找到时用库数据，找不到降级请求资料。BOOKING 状态变更用版本更新且整体事务；SCHEDULED 仅赛约更新无比赛版本比较。通知提交后异步且容错，不影响成功。
  → 已写入详细流程第 2、6 步、降级/并发/通知分支和技术线索
