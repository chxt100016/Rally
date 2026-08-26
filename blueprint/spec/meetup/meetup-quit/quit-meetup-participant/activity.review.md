# meetup.meetup-quit.activity.quit-meetup-participant 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些约球状态和报名状态允许退出，发布者能否退出？
  > 赛事约球一律拒绝；普通约球不检查 DRAFT/OPEN/CLOSED/ONGOING/FINISHED 或当前时间。只有 JOINED/REVIEWED/SKIPPED 可退，PENDING 和终止历史报 NOT_JOINED；发布者的 JOINED 报名也可退出。
  → 已写入触发条件、异常分支、业务动作 A1-A2、详细流程第 1-2 步与边界情况

- [Q2] 退出后报名、人数、约球状态和时间字段如何变化？
  > 目标报名只改为 QUIT，不更新 optTime/expiresAt；currentPlayers 按剩余 JOINED/REVIEWED/SKIPPED 重算。约球状态、创建者和其他字段不变，历史报名保留。
  → 已写入领域依赖、业务动作 A2/A4、详细流程第 3、6 步与边界情况

- [Q3] 处罚如何判断，是否实际扣分，并发与重复退出如何处理？
  > 距开始整小时差小于配置阈值即 PENALIZED，开始后负值也成立；结果未被应用层消费，不读取处罚分值、不扣信誉，仅有 TODO。重复退出报 NOT_JOINED；并发无版本条件，可能用旧集合重算覆盖。
  → 已写入业务动作 A3、详细流程第 4-5、7 步、边界情况与实现提示
