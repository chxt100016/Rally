# transaction-payment.payment-result-receipt.activity.record-payment-receipt 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 验真失败与未知事件是否都留痕？
  > 验真/解密失败不建日志；验真后的未知或无需推进事件建 RECEIVED 后标 PROCESSED。
  → 已写入活动契约、异常分支与详细流程第 1-3 步

- [Q2] 何时向微信返回成功或失败？
  > 所有处理无异常标 PROCESSED 并 200/SUCCESS；业务异常尽力 FAILED 并 500/FAIL 允许重试。
  → 已写入业务动作 A4 与详细流程第 4 步

- [Q3] 内部捕获对事务有什么影响？
  > 业务异常被回调服务内部捕获，异常前支付或赛事变化可能提交，日志尽力标 FAILED。
  → 已写入边界情况与实现提示
