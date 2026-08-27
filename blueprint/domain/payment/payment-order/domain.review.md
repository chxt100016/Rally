# @payment.payment-order 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 同一付款人对同一业务的活跃支付单如何唯一，关闭后是否允许重新建单？
  > PENDING 和 PAID 时 activeRefKey 固定为 bizType:refBizId:payerUserId 并唯一；CLOSED/FAILED 清空该键。关闭或失败后允许新建，已 PAID 不允许同业务再建活跃单。
  → I3、C1/C4/C5 与重建边界情况

- [Q2] 支付单建立后金额、付款人、业务引用与渠道是否允许修改，手续费如何计算？
  > 均不可修改。baseAmount 为正数，feeAmount=ceil(baseAmount*feeRate) 且非负，payAmount=baseAmount+feeAmount；费率由应用层作为建单参数传入。
  → 应付金额、I1/I2 与 C1

- [Q3] 并发回调、主动同步和超时关闭如何裁决首次支付与关闭？
  > 所有迁移用 bizId+PENDING 原状态 CAS 并检查影响行数；只有成功 CAS 到 PAID 的调用获得 FIRST_PAID，读到 PAID 返回 ALREADY_PAID，CLOSED/FAILED 拒绝。关闭也只有 CAS 成功后才可请求渠道关单。
  → I5、C3/C4 与并发边界情况

- [Q4] prepayId 的复用条件、支付时间和渠道流水如何保持一致？
  > 仅 PENDING 且 prepayId 非空、当前时间早于 prepayExpireTime 时可复用；PAID 必须同时有非空渠道流水和渠道成交时间，优先保存渠道时间而非本地处理时间。
  → 预支付/支付结果值对象、I4、C2/C3 与复用说明

- [Q5] 支付确认、预支付保存、关闭和失败是否应检查条件更新影响行数并重载，或新增渠道字段/摘要/有效期预校验？
  > 否。保持 main：payTime 用本地时间，渠道流水可空；PENDING 条件更新的影响行数可被调用方忽略且不补查；预支付按 bizId 普通更新；失败摘要原样；到期资格由活动判断。
  → I4/I5、C2-C5、并发边界与实现提示已统一为 main 的弱结果检查和本地时间语义。
