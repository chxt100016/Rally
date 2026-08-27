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

- [Q4] 回执 rawBody、终结更新和 FAILED remark 是否应增加脱敏、RECEIVED 条件 CAS 或非空/长度预校验？
  > 否。保持 main：rawBody 原样保存；按 bizId 普通更新且忽略影响行数，不加 RECEIVED 条件、不补查重试；remark 原样可空或超长，不预补默认值或截断。
  → I3/I4、C1/C2、边界与实现提示已统一为 main 的原样报文和普通更新语义。

- [Q5] 回调 ref_type=ORDER 时，缺失商户单号是否允许 ref_id 为 null/blank？
  > 允许。保持 main：未知、非成功或缺商户单号的回调仍保存 ref_type=ORDER，ref_id 原样可 null/blank，不要求两者同时有值。
  → 关联值对象、C1、边界与实现提示已明确 ref_type/ref_id 独立可选。
