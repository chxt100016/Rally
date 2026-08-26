# transaction-payment.timeout-order-close.flow.close-expired-orders 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务按什么开关和频率运行，哪些 PENDING 单会命中，expireTime 为空或刚好等于当前时间是否处理？
  > 仅 job.payment_timeout.enabled=true 时装配；按 job.payment_timeout.cron 运行，缺省 0 */5 * * * ?，未指定时区。一次查询全部 status=PENDING、expireTime 非空且严格早于扫描时刻的订单；空期限永不命中，刚好等于时刻要等后续扫描。无分页、上限或显式排序。
  → 已写入触发、接口契约、详细流程第 1-2 步与技术线索

- [Q2] 微信哪些交易结果和查单错误会被视为未付款并立即关单，是否有宽限或再次核实？
  > 只有渠道结果 isPaid=true 即微信 SUCCESS 视为已付；REFUND、NOTPAY、CLOSED、REVOKED、USERPAYING、PAYERROR、ACCEPT 等都直接视为未付。WechatPayClient 捕获的 ServiceException 也返回 paid=false 并关单；没有宽限或二次确认。SDK 未就绪等抛出异常则本笔不关单，留待后续扫描。
  → 已写入详细流程第 3、5 步、渠道分支与服务边界

- [Q3] 核实已付后的支付、报名、席位和赛事推进是否有统一事务，失败后是否会再次扫描？
  > 任务和 timeoutCheck 没有统一 @Transactional，各仓储写可分别提交。支付单若已标 PAID 后报名、席位或轮次推进失败，该单不再命中扫描，可能留下部分状态；若异常发生在支付单状态改变前且仍为 PENDING，则后续扫描可重试。
  → 已写入详细流程第 4、6 步、业务异常与技术线索

- [Q4] 本地条件关单未生效或微信关单失败时如何处理，关联 PAYING 报名和其他到期单如何收场？
  > 本地 close 只条件更新 PENDING，但返回值被忽略，随后仍请求渠道关单；并发已付款时本地可能保持 PAID 却仍发出关单请求。微信关单失败被吞掉并记录日志，本地已 CLOSED 不再扫描。未付关单不改关联 PAYING 报名；单笔未捕获异常由任务记录后继续其他订单。
  → 已写入详细流程第 5-7 步、并发与关单异常、服务边界
