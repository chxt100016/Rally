# meetup.meetup-square-list.activity.pack-meetup-square-cards 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] hasMore、total 与 TIME/DISTANCE 游标如何生成？
  > 候选数>pageSize 时 hasMore=true 并截前 pageSize，total 固定 null。仅 hasMore 且页非空才编码末项；TIME 为 [meetupId,startTime]，DISTANCE 为 [meetupId]，否则 nextCursor=null。
  → 已写入活动契约、业务动作 A1/A4、详细流程第 1、5-6 步与边界情况

- [Q2] 卡片距离、主标签与球场背景如何组装和降级？
  > 映射约球卡片基础字段，OPEN 主标签取 districtName。位置完整时重新按球面算法算公里距离；有 courtId 时读 type/surface，结合开始时段和固定晴天选择背景，缺失降级室外硬地晴天。
  → 已写入业务动作 A2-A3、详细流程第 2-4 步与边界情况

- [Q3] 球场缺失、位置不完整与末页如何处理？
  > 球场缺失或环境材质为空不阻断；lng/lat 只缺一项时不计算响应距离；空页或末页不生成游标，且不修改任何数据。
  → 已写入异常分支、详细流程第 4-6 步、边界情况与实现提示
