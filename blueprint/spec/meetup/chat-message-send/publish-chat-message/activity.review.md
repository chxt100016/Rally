# meetup.chat-message-send.activity.publish-chat-message 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 输入与参与资格的精确规则是什么，约球状态是否限制发送？
  > meetupId/content 必须非空白，contentType 必须为 TEXT/IMAGE/LOCATION，由流程先校验。创建者或报名为 PENDING/JOINED/REVIEWED/SKIPPED 可发送；约球状态不检查，其他报名状态报 NOT_JOINED。
  → 已写入触发条件、活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 发送者资料缺失与快照字段如何处理？
  > 按登录 userId 查 UserProfile，缺失报 TOKEN_INVALID。消息保存当前 nickname 和 avatarUrl 键作为不可回溯快照；不在保存前转换签名 URL。当前实现未额外校验昵称头像为空。
  → 已写入异常分支、领域依赖、业务动作 A2 与详细流程第 2 步

- [Q3] 消息编号、时间、幂等、事务与保存失败如何处理？
  > 每次生成新的雪花 bizId 与当前 LocalDateTime；没有客户端幂等键、内容查重、长度或频率限制。保存失败报 SYSTEM_ERROR；资格/资料读取和消息保存没有统一补偿需求，消息成功后由下游更新阅读状态。
  → 已写入领域依赖、业务动作 A3-A4、详细流程第 3-5 步与边界情况
