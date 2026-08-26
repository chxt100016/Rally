# meetup.review-skip.activity.skip-meetup-review 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些实际状态可跳过，是否受评价截止期限制？
  > 只允许实际 ONGOING/FINISHED；不读取 review.deadline_days，所以超过评价期限仍可跳过。
  → 已写入异常分支、业务动作 A1-A2、详细流程第 1-2 步、边界情况与实现提示

- [Q2] 是否要求参与资格，零条和多条 JOINED 报名如何处理？
  > 不要求创建者或参与者资格。按 userId+meetupId+JOINED 批量更新，零条仍成功，多条重复 JOINED 全部更新。
  → 已写入触发条件、活动契约、业务动作 A3、详细流程第 2-4 步与边界情况

- [Q3] 状态、optTime、既有评价比分与重复调用如何处理？
  > 命中记录转 SKIPPED 并写 optTime=now；其他报名、既有评价、比分、约球和档案不变。重复调用更新零行仍成功。
  → 已写入领域依赖、详细流程第 3-5 步与边界情况
