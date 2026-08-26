# transaction-payment.payment-result-receipt.flow.receive-wechat-payment-result 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 交易事件和未知事件分别如何验签、解密与留痕，哪些事件会直接按已处理应答？
  > event_type 以 TRANSACTION 开头才调用微信 NotificationParser 验签并解密，得到交易状态、outTradeNo 和 transactionId；未知事件不调用该解析器，只把原始 body 作为 UNKNOWN 留痕。UNKNOWN、已验真的非 SUCCESS 交易，以及 SUCCESS 但 outTradeNo 空白的事件均标 PROCESSED 并返回 SUCCESS，不推进支付单。
  → 已写入详细流程第 1-2、6-7 步、无需推进分支与技术线索

- [Q2] 首次成功与重复成功如何判定，是否核对金额、商户、付款人、币种、成功时间和渠道流水号？
  > 读取支付单时为 PENDING 即视为首次，为 PAID 则幂等返回；CLOSED/FAILED 报 PAYMENT_STATUS_ILLEGAL。现实现不核对 appid、mchid、金额、币种或付款身份，不使用微信 successTime，transactionId 也可为空；本地支付时间取处理时刻。条件更新的返回值被忽略，并发回执可能都按首次触发业务推进。
  → 已写入详细流程第 3、6 步、重复与状态异常、技术线索

- [Q3] 支付单已写 PAID 后关联赛事推进失败时事务是否回滚，回执重试能否再次推进关联业务？
  > handleCallback 虽有事务，但业务异常在方法内部 catch 后不再抛出，因此事务可能提交异常前已写的 PAID、席位或报名等部分变化，同时回执标 FAILED 并要求微信重试。若 PAID 已提交，重试会被当作重复付款，不再调用关联业务处理器，无法靠同一回执补齐。
  → 已写入详细流程第 7 步、失败分支、流程图与服务边界

- [Q4] 赛事报名和席位按什么状态推进，何时推进赛事轮次并淘汰其余资格赛等待报名？
  > 仅要求关联报名当前 status=PAYING，不要求原 stage=QUALIFY；先原子增加 currentFilledSlots，再改报名为 MAIN/WAITING、paidTime=本地当前时间、currentRound=firstMainRound(totalSlots)。资格赛所需完成数达到且席位填满时赛事进入首轮；席位满后所有仍为 QUALIFY/WAITING 的报名直接 ELIMINATED。
  → 已写入详细流程第 4-5 步、赛事异常与服务边界
