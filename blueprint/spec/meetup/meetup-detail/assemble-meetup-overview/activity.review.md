# meetup.meetup-detail.activity.assemble-meetup-overview 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] shareUserId、访问权限和约球不存在如何处理？
  > 当前 userId 来自登录上下文；除登录和 meetupId 存在外无参与资格限制。shareUserId 非 null 仅记录日志，不参与权限、归因或响应。约球不存在报 MEETUP_NOT_FOUND。
  → 已写入触发条件、活动契约、异常分支与详细流程第 1 步

- [Q2] 参与者视角、资料补充与缺失用户如何降级？
  > 创建者看到 PENDING 加 JOINED/REVIEWED/SKIPPED，非创建者仅后三种；批量查询参与者和创建者 user+tennis_profile。参与者资料缺失仍保留报名ID/userId/status/applyTime，展示字段空。创建者资料缺失时当前 gender 取值会空指针，归 SYSTEM_ERROR。
  → 已写入业务动作 A2-A3、详细流程第 3-6 步与边界情况

- [Q3] 背景、天气、发布次数与状态字段如何组装？
  > 同时返回存储 status 和按 start/end/当前时间推导的后续实际视图；背景按 courtId 查 rally_court 的 surface/type，缺失降级默认晴天背景；日出日落用 startTime、lat/lng 和上海时区算法。发布次数按 creatorId 统计其所有发布记录，不按状态过滤。头像键转签名URL。
  → 已写入 reads、业务动作 A1/A4-A5、详细流程第 2、5-8 步
