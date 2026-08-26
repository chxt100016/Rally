# transaction-payment.payment-status-sync.activity.deliver-payment-summary 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 对外状态如何映射？
  > PENDING→UNPAID、PAID→PAID、CLOSED/FAILED→CLOSED。
  → 已写入活动契约与详细流程第 3 步

- [Q2] 摘要交付哪些字段？
  > 支付单号、业务类型/ref、付款人及本金、手续费、总额，不含渠道细节。
  → 已写入业务动作 A2 与详细流程第 2 步

- [Q3] 摘要是否再次查渠道？
  > 不查，只读取本地最新状态；终态订单可直接返回。
  → 已写入详细流程第 1、4 步与边界情况
