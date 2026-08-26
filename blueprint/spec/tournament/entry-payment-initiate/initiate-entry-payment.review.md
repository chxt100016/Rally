# tournament.entry-payment-initiate.flow.initiate-entry-payment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 支付发起的身份、报名状态和正赛席位前置条件是什么？
  > 只为 UserContext 当前用户本人处理；赛事和该用户报名必须存在，entry.assertCanPay 只要求 status=PAYING，tournament.assertSlotsNotFull 要求 currentFilledSlots<totalSlots，不额外校验赛事状态或报名 stage。
  → 已写入触发、详细流程第 1-2 步及资格异常分支

- [Q2] 既有支付单何时复用、何时关闭并新建，金额与渠道从哪里来？
  > PaymentDomainService.createSingle 以 TOURNAMENT_ENTRY_FEE、entry bizId、付款人复用可用订单；过期 PENDING 先尝试关闭再新建。金额取 tournament.entryFee.intValue()，手续费为 0，渠道固定 WECHAT。
  → 已写入详细流程第 3-4 步与服务边界

- [Q3] 同步成功交付什么，是否会推进支付单和报名状态，外部调用失败如何收场？
  > 成功返回 paymentId 与微信小程序预支付/签名字段，只保存订单和 prepayId 有效期；不会置 PAID，也不推进报名、席位或轮次。应用方法虽有事务，但微信下单/关单是外部副作用，异常时本地回滚不能撤销已发生的微信侧结果。
  → 已写入接口契约、详细流程第 5 步、外部失败分支与事务线索
