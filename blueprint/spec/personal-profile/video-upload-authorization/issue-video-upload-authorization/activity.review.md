# personal-profile.video-upload-authorization.activity.issue-video-upload-authorization 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 数量上限检查包括哪些视频和哪些缺档情况？
  > 只在档案存在且 videos 非空时比较已登记条数>=配置上限；无档案/null/空列表跳过，未登记文件与已签令牌不计数。
  → 已写入业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 配置非法时数量与大小如何处理？
  > 整数配置均降级为0；非空列表因此被判超限，无档案或空列表仍签发0MB令牌。
  → 已写入异常分支、详细流程第 3 步与边界情况

- [Q3] 返回前缀与最终七牛 scope 是否一致？
  > 不一致；策略先写用户视频前缀，但 SDK 以 bucket 和 null key 签发，最终 scope 为整个桶。返回 keyPrefix 仅是建议，时长60秒也未进入策略。
  → 已写入业务动作 A3、详细流程第 4-5 步与实现提示
