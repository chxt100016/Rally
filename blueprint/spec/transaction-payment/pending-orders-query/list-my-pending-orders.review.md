# transaction-payment.pending-orders-query.flow.list-my-pending-orders 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 待支付清单按什么身份和内部状态筛选，已过 expireTime 但仍为 PENDING 的订单是否返回？
  > 只按 payerUserId=当前登录用户且 status=PENDING 查询，不按业务类型或渠道过滤。expireTime 不参与条件，所以已过期但后台尚未关单的 PENDING 订单仍返回；PAID、CLOSED、FAILED 不返回。
  → 已写入详细流程第 1-2 步、查询技术线索与服务边界

- [Q2] 列表是否分页、限量或排序，无结果时返回什么？
  > 仓储一次返回全部结果，没有分页参数、数量上限或显式 order by，顺序不承诺；没有记录时返回成功的空数组。
  → 已写入详细流程第 4 步、接口契约与服务边界

- [Q3] 摘要交付哪些字段，查询是否会核实微信状态、关闭过期单或推进关联业务？
  > 交付 paymentId、refBizId、payerUserId、baseAmount、feeAmount、payAmount 和由 PENDING 映射的 UNPAID；不交付 bizType、channel、expireTime 或描述。查询纯只读，不查微信、不关单、不改状态、不推进报名。
  → 已写入详细流程第 3 步、技术线索与服务边界
