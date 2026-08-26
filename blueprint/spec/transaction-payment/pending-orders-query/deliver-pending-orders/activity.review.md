# transaction-payment.pending-orders-query.activity.deliver-pending-orders 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 待支付筛选是否考虑过期或渠道状态？
  > 不考虑；只按本人和本地 PENDING，不查 expireTime 或渠道。
  → 已写入活动契约与详细流程第 1-2 步

- [Q2] 查询是否分页排序？
  > 不分页、不限量、无明确排序，无结果返回空数组。
  → 已写入活动契约、详细流程第 4 步与边界情况

- [Q3] 哪些状态错位会出现在清单？
  > 已过期但未关闭、渠道已付但本地未确认的订单仍显示 UNPAID。
  → 已写入详细流程第 2-3 步与边界情况
