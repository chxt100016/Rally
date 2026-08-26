# meetup.meetup-join.activity.grant-meetup-notification-quota 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 授权场景的解析、白名单和重复项规则是什么？
  > 按 NoticeScene 枚举名逐项解析，非法值忽略，null/空列表不建账；当前不按 MEETUP 维护白名单，也不去重，因此其他已知场景和重复项都可形成额度。
  → 已写入活动契约、业务动作 A1、详细流程第 1、4 步与边界情况

- [Q2] 直接满员时为什么排除 JOIN_SUCCESS，其他场景是否变化？
  > 仅 JOINED 且更新后满员时过滤全部 JOIN_SUCCESS，避免与随后 TEAM_SUCCESS 重复；不自动增加 TEAM_SUCCESS，也不改变其他场景或 PENDING 分支。
  → 已写入活动契约、业务动作 A2、详细流程第 2 步与边界情况

- [Q3] 额度如何建账，失败是否回滚报名？
  > 每个保留场景生成独立雪花流水，记录 userId/MEETUP/meetupId/scene/templateId 并置 UNUSED 后批量保存。grant 捕获所有异常只记日志，因此失败不回滚报名或群聊；成功流水随外层事务提交。
  → 已写入异常分支、领域依赖、业务动作 A3-A4、详细流程第 3、5-6 步与实现提示
