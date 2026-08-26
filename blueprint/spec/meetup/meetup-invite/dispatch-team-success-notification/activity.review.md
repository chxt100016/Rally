# meetup.meetup-invite.activity.dispatch-team-success-notification 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时触发组团通知，候选人是否包括创建者和被邀请人？
  > 聊天加入后用聚合 isFull（currentPlayers>=maxPlayers）判断；未满不触发任何邀请成功通知。满员时事务提交后异步，候选为所有 JOINED/REVIEWED/SKIPPED 有效参与者，通常包含创建者报名和被邀请人。
  → 已写入触发条件、活动契约、业务动作 A1-A2 与详细流程第 1-3 步

- [Q2] 额度选择、成员复核与并发消费如何处理？
  > 按 MEETUP/meetupId/TEAM_SUCCESS 查询 UNUSED，每用户处理第一条；发送前 shouldNotice，明确已退出跳过保留额度，复核异常 fail-open；CAS 到 SENDING 失败跳过。
  → 已写入领域依赖、业务动作 A2-A3 与详细流程第 3-4 步

- [Q3] 发送内容、失败隔离与重复满员触发如何处理？
  > 使用 teamSuccessData 的活动摘要和微信 TEAM_SUCCESS 模板；渠道/身份/发送/回写失败记录且不回滚邀请。接口不等待。后续重复操作再次满足 isFull 可能再次触发并消费另一条额度，无通知级幂等键。
  → 已写入异常分支、业务动作 A4、详细流程第 5-6 步与边界情况
