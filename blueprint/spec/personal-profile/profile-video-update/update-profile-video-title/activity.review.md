# personal-profile.profile-video-update.activity.update-profile-video-title 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 目标不存在、重复 key 和 videos 为 null 时如何处理？
  > videos 为 null 或无命中都不改但仍保存成功；重复 key 只修改列表中的第一项。
  → 已写入活动契约、详细流程第 2-4 步与边界情况

- [Q2] title 的 null、空串和纯空白语义是什么？
  > 均原样持久化，不在更新时规范；后续完整档案展示时统一显示未命名。
  → 已写入业务动作 A2、详细流程第 3 步与边界情况

- [Q3] 异常存量项和外部文件如何处理？
  > 目标前若有 null 项或 null key，equals 比较可能失败；活动不上传、替换或删除七牛文件。
  → 已写入异常分支、边界情况与实现提示
