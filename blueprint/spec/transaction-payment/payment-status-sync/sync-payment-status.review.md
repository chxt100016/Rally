# transaction-payment.payment-status-sync.flow.sync-payment-status 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些本地支付状态会跳过微信查单，PAID、CLOSED、FAILED 分别如何映射为对外摘要？
  > 只有 PENDING 才查询微信；PAID 直接返回对外 PAID，CLOSED 与 FAILED 都映射为对外 CLOSED，PENDING 未核实为已付时返回 UNPAID。已付单不会借此入口重新补推关联业务，关闭或失败单也不会核对迟到付款。
  → 已写入详细流程第 2 步、本地状态分支与服务边界

- [Q2] 微信查单返回未支付、业务错误或渠道配置不可用时，是否区分失败与真实未支付？
  > 微信 SDK 的 ServiceException 被渠道客户端转换为 paid=false，因此与真实未支付一样返回 UNPAID，支付单保持 PENDING。渠道未配置或不受支持会抛 PAYMENT_CHANNEL_NOT_SUPPORTED；其他未被转换的异常终止请求，不返回正常摘要。
  → 已写入详细流程第 3 步、渠道异常与服务边界

- [Q3] 微信确认已付后关联报名或席位推进失败时，本地支付单是否回滚，微信付款事实如何收场？
  > PaymentAppService.syncPayStatus 的外层 @Transactional 不捕获推进异常；报名不存在、赛事不存在、状态不符、满位或保存失败会回滚本次支付单、席位、报名和赛事本地变化。微信渠道的已付款事实无法回滚；本地单仍 PENDING 时后续同步可再次查询并重试。
  → 已写入详细流程第 4-7 步、业务异常与技术线索

- [Q4] 首次付款与并发同步如何判定，报名、席位、赛事轮次和其余资格赛报名如何推进？
  > 读取时 PENDING 即按首次付款，markPaid 的数据库条件更新结果未检查；并发同步都可能继续触发业务推进，虽占位更新会防止超过总量，仍可能为同一报名多占席位。正常单次流程先占位，再把 PAYING 报名改 MAIN/WAITING 并设首轮；资格赛完成且席位满时推进赛事，满位后淘汰其余 QUALIFY/WAITING 报名。
  → 已写入详细流程第 4-6 步、技术线索与服务边界

- [Q5] 支付摘要是否应新增对外 bizType 字段？
  > 否。保持 main 现有 PaymentOrderSummaryDTO，不新增 bizType，只交付 refBizId。
  → 接口契约已移除 bizType 并明确保持现有 DTO。
