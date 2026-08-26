# transaction-payment.receipt-recovery.flow.recover-stalled-receipts 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 任务按什么开关和频率执行，补偿阈值、扫描范围、批量上限和顺序是什么？
  > 仅 job.payment_callback_recover.enabled=true 时装配；按 job.payment_callback_recover.cron 运行，缺省 0 */10 * * * ?，未指定时区。每次查询 CALLBACK/RECEIVED 且 createTime<now-5分钟的全部日志，五分钟阈值写死，没有分页、数量上限或显式排序。
  → 已写入触发、接口契约、详细流程第 1-2 步与技术线索

- [Q2] 关联无效、支付单不存在或支付单已非 PENDING 时，回执分别如何结束？
  > refType!=ORDER 或 refId 空白直接标 PROCESSED；找不到支付单标 FAILED，失败原因固定 payment_order_not_found；支付单为 PAID、CLOSED、FAILED 时均不查微信并标 PROCESSED，也不尝试补齐已付单可能缺失的关联业务。
  → 已写入详细流程第 3-4 步、订单关联分支与服务边界

- [Q3] 微信未确认付款或查单返回业务错误时为何标 PROCESSED，后续是否还会自动重查？
  > 微信明确未付以及渠道客户端捕获的 ServiceException 都表现为 paid=false，支付单保持 PENDING，但回执仍标 PROCESSED。扫描只选 RECEIVED，因此该回执之后不会再次自动核实；配置不可用等向外抛出的异常才会标 FAILED。
  → 已写入详细流程第 5 步、渠道分支与服务边界

- [Q4] 单条支付与赛事推进是否有事务保证，异常部分状态、回执 FAILED 和其他记录如何处理？
  > 任务、recover 和 recoverIfPaid 均无统一 @Transactional，支付单、席位、报名、赛事和回执更新可能分别提交；中途异常可留下 PAID 或已占席位等部分状态。单条异常由循环捕获并尝试标 FAILED 后继续；FAILED 不再扫描。若标 FAILED 本身再次抛错，当前扫描会中断。
  → 已写入详细流程第 6-7 步、异常分支与技术线索
