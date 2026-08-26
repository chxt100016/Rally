# meetup.meetup-invite.activity.register-invited-participant 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 邀请权限、活动状态、被邀请人资料和准入规则如何处理？
  > 只要求操作人为创建者、加载时未满员、被邀请人无 PENDING/JOINED/REVIEWED/SKIPPED 报名。不校验约球状态/时间/类型/joinMode，不查被邀请用户是否存在、资料、性别、NTRP、信誉或时间冲突。
  → 已写入触发条件、领域依赖、业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 新报名与历史报名、当前人数如何保存？
  > 构造新 RegistrationData 自动生成雪花 bizId，状态直接 JOINED；REJECTED/WITHDRAWN/QUIT 历史保留并允许新建另一条。聚合整体保存时以 bizId upsert全部报名，并把 currentPlayers 重算为创建者+JOINED/REVIEWED/SKIPPED 数量；接口不返回 registrationId。
  → 已写入业务动作 A3-A5、详细流程第 3-6 步与边界情况

- [Q3] 重复和并发邀请如何处理，事务何时提交？
  > 串行重复因 active registration 报 ALREADY_JOINED；没有请求幂等键。并发请求可能基于旧集合同时通过，数据库只有 bizId 唯一而无 meetup+user 唯一，可形成重复报名并突破上限。报名、聊天加入同属应用事务，聊天失败会回滚。
  → 已写入详细流程第 5 步、边界情况与实现提示
