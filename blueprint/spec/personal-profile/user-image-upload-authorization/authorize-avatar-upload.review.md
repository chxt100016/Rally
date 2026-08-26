# personal-profile.user-image-upload-authorization.flow.authorize-avatar-upload 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 头像 key、扩展名、大小上限和同秒重复请求分别如何确定？
  > 固定 key 为 avatar/{userId}_{yyyyMMddHHmmss}.jpeg，精确到秒；同秒重复得到相同 key。大小读取 USER_AVATAR_MAX_SIZE_MB，缺配置默认 5，非法整数为 0。
  → 已在详细流程、技术线索和契约明确 key、扩展名、配置大小及同秒复用。

- [Q2] 上传策略中的 600 秒 deadline 与签发参数 3600 秒哪个实际生效？
  > 代码 policy 先 put 当前时间+600，但随后 Auth.uploadToken(bucket,key,3600,policy) 会按 SDK 签发期限覆盖/确定 deadline，当前有效期为 3600 秒。
  → 已在详细流程和技术线索明确当前令牌实际按 3600 秒签发。

- [Q3] 授权是否校验账户档案、实际图片格式与上传结果，并保存或清理头像资源？
  > 均不校验。只取 UserContext，不查 user/profile；令牌不限制 MIME 或图像内容，不确认上传结果，不写头像字段或资源记录，也不删除历史头像。
  → 已在详细流程、异常说明和服务边界明确不查档案、不验文件、不持久化或清理。
