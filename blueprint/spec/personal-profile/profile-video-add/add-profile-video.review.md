# personal-profile.profile-video-add.flow.add-profile-video 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 没有网球档案或档案为 TBC 时能否新增，成功响应是否展示新增视频？
  > 基础用户不存在抛 TOKEN_INVALID；有用户但 profile=null 时 userProfile.addVideo 解引用空 profile，系统异常且不建档。TBC 可追加并保存，但 getMyProfile 的 hasProfile=false，响应 video 为 null，不展示新增项。
  → 已在详细流程、契约和异常分支明确无档案失败、TBC 可保存但响应不展示视频。

- [Q2] 是否执行视频数量、大小、时长、文件类型、存在性与资源归属校验？
  > 全部不执行。只用 @NotBlank 校验 key，并调用 buildSignedUrl 检查能否按配置构址；不访问七牛文件，也不读取或执行 maxCount、maxSizeMb、maxSecond。
  → 已在契约、详细流程和服务边界明确除非空白与构址外不执行资源和限制校验。

- [Q3] 重复 key 和空白标题如何保存与展示？
  > 不去重，相同 key 每次都追加为独立项。title 无校验，null、空串或空格原样持久化；NORMAL/UNDER_REVIEW 返回时用 StringUtils.isBlank 显示为“未命名”。
  → 已在详细流程、契约和异常说明明确重复 key 追加、空白标题原样保存并展示为未命名。

- [Q4] 无扩展名 key 在 TBC、NORMAL、UNDER_REVIEW 下分别如何表现？
  > 预签名不要求扩展名。TBC 返回不构建视频分组，所以可成功保存无扩展名 key；NORMAL/UNDER_REVIEW 会遍历全部视频并 buildCover，lastIndexOf 为 -1 导致异常，事务回滚本次追加。
  → 已在详细流程和异常说明明确 TBC 不构建封面可成功，NORMAL/UNDER_REVIEW 失败并回滚。

- [Q5] 保存视频后返回档案聚合失败是否回滚追加？
  > 会。uploadVideo 标注 @Transactional，保存后同步 getMyProfile；城市、统计、评分、配置、任一视频封面或七牛签名等运行时异常都会回滚完整视频列表更新。
  → 已在详细流程、流程图和异常分支明确保存与聚合同事务，聚合失败回滚追加。
