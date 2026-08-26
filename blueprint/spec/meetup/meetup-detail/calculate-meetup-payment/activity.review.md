# meetup.meetup-detail.activity.calculate-meetup-payment 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时返回 payment，角色与计算人数如何决定？
  > costItems 为 null/空时返回 null。创建者 COLLECTOR，有效参与者 PAYER，其余 STRANGER。实际状态 OPEN 用 maxPlayers 作为 calculatedPlayerCount，其他状态用创建者加有效报名的已批准人数。
  → 已写入触发条件、活动契约、业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 总额、平均分摊与人时分摊的舍入和异常边界是什么？
  > 总额按 costItems.totalAmount int 求和，null 会 SYSTEM_ERROR。无非空 hourlyAllocations 时 AVERAGE：人数>0 用整数除法截断，否则0。人时模式：hourlyRate=total/duration 保留2位 HALF_UP；包含当前用户的每段按 rate*duration/userIds.size 保留2位 HALF_UP 后累加，最终 intValue 截断；未包含为0且说明空。duration=0/null、空用户列表等可导致 SYSTEM_ERROR。
  → 已写入业务动作 A2-A3、详细流程第 3-6 步与边界情况

- [Q3] 分摊说明和收款码对哪些角色返回，资源缺失如何处理？
  > 说明只汇总包含当前用户的段，按参与人数首次出现顺序合并 duration，格式“4人2小时、3人1小时”。无论 COLLECTOR/PAYER/STRANGER，只要创建者 user_ext 中 PAYMENT_CODE 存在就返回签名URL；缺失则 paymentCodeUrl 空，签名失败归 SYSTEM_ERROR。
  → 已写入 reads、业务动作 A3-A5、详细流程第 6-8 步与边界情况
