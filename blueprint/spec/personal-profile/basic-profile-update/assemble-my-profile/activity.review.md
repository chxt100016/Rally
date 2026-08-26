# personal-profile.basic-profile-update.activity.assemble-my-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 档案状态如何决定返回分组，基础资料如何组装？
  > 无档案为 NONE，TBC 不完整，NORMAL/UNDER_REVIEW 完整。基础 user 始终返回并签名头像、解析城市；不完整时 stats/level/score/video 为 null。
  → 已写入活动契约、业务动作 A1-A2、详细流程第 1-3 步与边界情况

- [Q2] 统计、等级、评分和视频的精确读取与计算是什么？
  > 完整档案统计关注/粉丝和 REVIEWED/SKIPPED 完成约球；NTRP、冷却和核查场次组等级提示，配置驱动综合评分；视频逐项签名 URL/封面并读取上传限制。
  → 已写入业务动作 A3-A5、详细流程第 4-6 步与边界情况

- [Q3] 城市、NTRP、视频、签名和配置异常是否回滚更新？
  > 未知非空城市、null NTRP、null videos、无扩展名资源或签名故障可失败；配置解析按工具降级。异常向上传播并回滚同事务基础资料更新。
  → 已写入异常分支、详细流程第 6-7 步、边界情况与实现提示
