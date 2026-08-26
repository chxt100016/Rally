# personal-profile.player-home.activity.query-player-review-summary 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 评价查询范围与 total 如何计算？
  > 按目标 to_user_id 读取全部历史评价；total 为 LEVEL_VOTE 记录数、ATTENDANCE_VOTE 记录数与 TAG 原始非空拆分片段数之和。
  → 已写入活动契约、业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 高频标签如何规范、计数与截取？
  > 按英文逗号拆分后 trim、排空、按名称累计，同条重复也重复计数，次数降序取五项，同次数无稳定次级排序。
  → 已写入业务动作 A3、详细流程第 3 步与边界情况

- [Q3] 主页返回哪些评价字段，无评价如何处理？
  > 只赋 total 和前五标签，三类明细计数保持 null；无评价返回 total=0 与空列表。
  → 已写入详细流程第 4 步与边界情况
