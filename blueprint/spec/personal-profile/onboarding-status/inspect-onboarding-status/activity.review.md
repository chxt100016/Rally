# personal-profile.onboarding-status.activity.inspect-onboarding-status 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 用户与档案不存在时分别返回什么结果？
  > 基础用户不存在报 TOKEN_INVALID；用户存在但无网球档案是正常的 NONE，不在检查活动内创建档案。
  → 已写入活动契约、异常分支与详细流程第 1-2 步

- [Q2] 已有档案时是否更新或规范化状态？
  > 直接返回持久化的 TBC、NORMAL 或 UNDER_REVIEW，不更新档案字段，也不让 is_under_review 修正顶层状态。
  → 已写入业务动作 A2、详细流程第 2-3 步与边界情况

- [Q3] 检查结果代表哪个时点，是否包含初始化后的终态？
  > 结果代表本次读取开始时点；NONE 只触发后续活动，后续写入 TBC 不改变已产出的 NONE。
  → 已写入活动契约、详细流程第 3 步与边界情况
