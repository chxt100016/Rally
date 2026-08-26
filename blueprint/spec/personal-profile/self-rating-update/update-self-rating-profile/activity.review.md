# personal-profile.self-rating-update.activity.update-self-rating-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 冷却天数如何分档与计算剩余值？
  > 按 ntrpUpdatedAt 到当前的整日差；可信度 null/<30、30-59、>=60 分别用低中高配置，不足时报 cooldown-daysSince。
  → 已写入业务动作 A1、详细流程第 2 步与边界情况

- [Q2] 哪些修改触发核查，未触发是否解除既有核查？
  > delta=new-old，旧值 null 时 delta=0；达到阈值才设 UNDER_REVIEW、标记和所需场次，降低/同值/小涨不触发也不解除已有核查。
  → 已写入业务动作 A2、详细流程第 3 步与边界情况

- [Q3] 档案实际持久化哪些核查字段，调用顺序是什么？
  > 触发时当前 Java 先写核查日志，再更新 NTRP、时间、状态和标记；仓储未持久化 reviewRemainingMatches。
  → 已写入业务动作 A3-A4、详细流程第 4-5 步与实现提示
