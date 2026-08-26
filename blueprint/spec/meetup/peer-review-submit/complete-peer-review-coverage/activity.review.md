# meetup.peer-review-submit.activity.complete-peer-review-coverage 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 所需评价目标和已覆盖目标分别如何计算？
  > 所需目标是除本人外全部 JOINED/REVIEWED/SKIPPED 报名用户；已覆盖目标取本人在该约球全部评价记录的 toUserId 去重。containsAll 即完成。
  → 已写入领域依赖、业务动作 A2-A3、详细流程第 2-5 步

- [Q2] 哪些报名状态短路或可更新，空目标和 PENDING 如何处理？
  > 本人已有 REVIEWED/SKIPPED 即短路；所需目标为空直接尝试完成。数据库只把 JOINED 改 REVIEWED 并写 optTime，PENDING 可保存评价但更新零行仍成功。
  → 已写入业务动作 A1/A4、详细流程第 1、3、6-7 步与边界情况

- [Q3] 单维度、自评、并发参与者变化和更新零行如何处理？
  > 任一单维度即可覆盖目标；自评或无关目标保存但不能替代缺失目标。判断使用加载时参与者集合，并发新成员不撤销完成；条件更新零行不检查也不报错。
  → 已写入详细流程第 4-7 步、边界情况与实现提示
