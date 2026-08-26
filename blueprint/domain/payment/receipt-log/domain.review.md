# @payment.receipt-log 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 重复渠道回调是否复用一条日志，还是每次验真成功的接收都独立留痕？
  > 每次验真成功的接收都建立独立 CALLBACK 日志，不按商户单号去重；支付幂等由 payment-order 负责。
  → 边界情况中的重复回调语义

- [Q2] RECEIVED、PROCESSED、FAILED 如何迁移，终态是否可重新打开？
  > CALLBACK 从 RECEIVED 单向进入 PROCESSED 或 FAILED，终态不可改写或重新打开；需要重试时由新的恢复执行记录或人工机制表达。COLLECT/PREPAY 建立即 PROCESSED。
  → 状态、I2/I3、C1/C2 与恢复说明

- [Q3] rawBody 与 remark 的可变性、敏感信息和失败原因长度如何约束？
  > rawBody 是验真后审计快照，建立后不可修改，写入前脱敏凭据和个人敏感字段；remark 仅在终结时写摘要，最多 255 字符，不保存堆栈或密钥。
  → I4、C1/C2 与实现提示
