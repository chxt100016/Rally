# content-production.tournament-image-maintenance.activity.generate-tournament-images 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 原图校验与两种 JPEG 派生规则如何定义？
  > 请求必须有非空 tournamentId 和可读取图片，HTTP 请求上限 5MB；主图从原始字节按 JPEG 质量 0.75 压缩，背景图独立从原始字节以 50KB 为目标压缩，不从主图二次压缩。
  → 已写入活动契约、业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 七牛资源键是否固定，重复上传和两次上传之间失败如何处理？
  > 目录固定为 tournament，主图文件名 tournamentId.jpg，背景图 tournamentId_background.jpg。重复上传覆盖相同键；先主图后背景图，任何上传失败报 SYSTEM_ERROR，已写入对象不删除且不回滚。
  → 已写入领域依赖、业务动作 A3、详细流程第 3-4 步与边界情况

- [Q3] 活动成功返回什么，是否生成访问地址或写赛事表？
  > 仅返回 imageKey 与 backgroundKey，不生成访问 URL，不写 tour_tournament；数据库绑定由下游活动完成，Qiniu RPC snapshot 缺失时以既有客户端契约为准。
  → 已写入活动契约、业务动作 A4、详细流程第 5 步与实现提示
