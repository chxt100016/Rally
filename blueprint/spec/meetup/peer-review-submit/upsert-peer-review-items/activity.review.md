# meetup.peer-review-submit.activity.upsert-peer-review-items 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 评价列表、各维度值和 TAG 的精确校验是什么？
  > LEVEL 只收 HIGHER/SAME/LOWER，ATTENDANCE 只收 ON_TIME/LATE/NO_SHOW，TAG 仅要求非 null。reviews 无非空注解：null 或 null item 会系统失败，空列表合法；TAG 可空白、自定义和逗号串。
  → 已写入触发条件、活动契约、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 评价人与目标用户的资格、阶段和截止时间如何校验？
  > 评价人须是创建者或有 PENDING/JOINED/REVIEWED/SKIPPED 报名，实际状态须 ONGOING/FINISHED，且不晚于 endTime+deadlineDays。目标用户不验证账户、本人或同场资格。
  → 已写入异常分支、领域依赖、业务动作 A2、详细流程第 3-4 步与边界情况

- [Q3] 唯一组合、重复维度、并发和事务如何处理？
  > 唯一键为 meetup/from/to/type，存在更新 value，否则新建雪花记录；同请求重复维度后项覆盖前项。并发首次插入由唯一约束裁决且不重试；所有评价和报名推进同事务。
  → 已写入领域依赖、业务动作 A3-A5、详细流程第 5-7 步、边界情况与实现提示
