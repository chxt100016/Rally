# meetup.meetup-close.activity.dispatch-meetup-cancel-notifications 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 候选接收人、执行时机与接口等待语义是什么？
  > 核心事务提交后在线程池异步执行；候选为除创建者外 JOINED/REVIEWED/SKIPPED 有效参与者。接口不等待发送完成，通知结果不改变关闭成功。
  → 已写入触发条件、活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 额度选择、并发占用和发送前成员复核如何处理？
  > 查询 MEETUP/meetupId/MEETUP_CANCEL/UNUSED，按仓储顺序每用户只处理第一条（约定最早）；复核 shouldNotice，退出者跳过且保留 UNUSED，复核异常 fail-open；CAS UNUSED->SENDING 失败则跳过。
  → 已写入领域依赖、业务动作 A2-A3、详细流程第 2-4 步与边界情况

- [Q3] 渠道缺失、微信失败、状态回写失败以及通知内容如何处理？
  > 渠道缺失标 FAILED；发送结果回写 SENT/FAILED，异常尝试标 FAILED，所有异常只记日志不回滚。内容为约球名称、时间、地点和固定原因“创建人取消”；账号/微信身份缺失由 notifier 形成失败。
  → 已写入活动契约、业务动作 A4、详细流程第 5-6 步与实现提示
