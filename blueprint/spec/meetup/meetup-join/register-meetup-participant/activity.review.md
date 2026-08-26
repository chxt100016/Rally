# meetup.meetup-join.activity.register-meetup-participant 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 资料完整性、约球状态与准入缺失值分别如何判定？
  > 账户须存在，昵称头像不能仍为默认值，档案不得为 NONE/TBC，核查期可用；拒绝满员、关闭、已结束/开始/进行中、本人创建及活跃重复报名，但未单拒 DRAFT。性别、信誉或 NTRP 缺失时对应准入放行，未公开性别不满足限男女。
  → 已写入触发条件、领域依赖、业务动作 A1-A3、详细流程第 1-3 步与边界情况

- [Q2] 报名状态、自动撤回时间、人数与历史报名如何保存？
  > DIRECT 建 JOINED，APPROVAL 建 PENDING；构造器生成雪花报名编号，autoWithdrawAt 原样存 expiresAt 且无时序校验。REJECTED/WITHDRAWN/QUIT 历史保留；currentPlayers 重算为创建者加 JOINED/REVIEWED/SKIPPED，PENDING 不占人数。
  → 已写入活动契约、业务动作 A4-A5、详细流程第 4-5 步与边界情况

- [Q3] 分享归因、事务边界与并发重复如何处理？
  > shareUserId 只记日志，不持久化或参与权限。报名与直接加入群聊同属应用事务，群聊失败会回滚。无幂等键和 meetup+user 唯一约束，并发可能重复报名或超员。
  → 已写入详细流程第 6-7 步、边界情况与实现提示
