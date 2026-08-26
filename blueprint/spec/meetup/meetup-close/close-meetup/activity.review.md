# meetup.meetup-close.activity.close-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 关闭资格在有无其他参与者时如何区别，实际状态如何计算？
  > 仅创建者可关，TOURNAMENT 类型禁止。实际状态按当前时间懒计算。无其他 JOINED/REVIEWED/SKIPPED 参与者时除 CLOSED 外都可关，包括 FINISHED；有其他有效参与者时 FINISHED/CLOSED 禁止，其余可关。
  → 已写入异常分支、领域依赖、业务动作 A1-A2 与详细流程第 1-3 步

- [Q2] 关闭保存范围和事务边界是什么？
  > 把 rally_meetup.status 保存为 CLOSED，报名、聊天、比分、评价、费用均不变。应用方法有事务；约球保存或随后同事务内配置读取/处理抛异常会回滚关闭，提交失败不触发 afterCommit 通知。
  → 已写入业务动作 A3-A5、详细流程第 4、7-8 步与边界情况

- [Q3] 处罚什么时候计算，四档边界和值如何落地？
  > 仅 currentPlayers>1 时读取 24h外、12-24h、6-12h、6h内四个系统配置，按 startTime 距当前时间选档；当前 calculateCancelPenalty 实现只返回配置值，若>0仅日志记录，不扣信誉分、不写处罚记录。
  → 已写入业务动作 A4、详细流程第 5-6 步与边界情况
