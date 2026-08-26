# personal-profile.profile-video-update.flow.update-profile-video-title 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 目标 key 不存在或列表中有多个相同 key 时修改哪些项并如何返回？
  > stream.findFirst 只修改首个 key 相等项；重复项其余标题不变。没有命中时列表完全不变，但仍保存档案、聚合并返回成功，没有“视频不存在”错误。
  → 已在触发、详细流程、契约和服务边界明确无命中成功、重复 key 只改首项。

- [Q2] title 为 null、空串或纯空白时如何持久化和展示？
  > UpdateVideoCmd 只校验 key，title 没有约束；命中后 null、空串或空格原样写入视频 JSON。返回时 StringUtils.isBlank 将三者展示为“未命名”，持久化值不会替换成该文案。
  → 已在契约、详细流程和服务边界明确空标题原样保存、响应展示为未命名。

- [Q3] 没有网球档案、videos 为 null、空列表或含 null key 项时如何处理？
  > 无基础用户抛 TOKEN_INVALID；有用户无 profile 时解引用空对象，系统异常。profile.videos=null 时领域方法直接返回并可在 TBC 成功；空列表同样无命中成功。遍历时 null 视频项或其 key=null 在 equals 调用处异常。
  → 已在详细流程和异常分支区分无档案、null/空列表与 null 项或 key。

- [Q4] 保存标题后完整档案聚合失败是否回滚，是否会修改七牛文件？
  > 会。updateVideo 标注 @Transactional，保存后同步 getMyProfile，聚合运行时异常会回滚档案更新。流程从不调用七牛删除或上传，只为响应生成签名地址和封面。
  → 已在详细流程、流程图和异常分支明确同事务回滚，且不修改七牛文件。
