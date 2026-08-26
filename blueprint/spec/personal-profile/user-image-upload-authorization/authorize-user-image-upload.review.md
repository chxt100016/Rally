# personal-profile.user-image-upload-authorization.flow.authorize-user-image-upload 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] type 允许哪些值，空白、路径字符和同秒重复请求如何影响 key？
  > 没有允许值校验，Controller 只要求参数存在；空串、空格、斜杠等原样进入 key。key 精确到秒，同 user+type 同秒重复相同，可能覆盖同一位置。
  → 已在触发、契约、详细流程和异常说明明确 type 仅要求参数存在且原样拼接。

- [Q2] 指定类型图片的大小、格式、令牌期限和上传地址如何确定？
  > 固定 .jpg 和 10 MB，实际内容类型不限制；精确 key 授权 3600 秒，上传地址固定 https://up-z0.qiniup.com，resourceUrl 也签名 3600 秒。
  → 已在契约、详细流程和技术线索明确固定格式、大小、期限和上传地址。

- [Q3] 响应为何含 keyPrefix、maxDurationSec，授权是否会保存用途或更新档案？
  > 复用 VideoTokenVO，固定 key 模式未设置 keyPrefix，因此为 null；int maxDurationSec 未设置所以为 0。流程不保存 type、key 或授权记录，不更新 user/profile。
  → 已在契约、详细流程和服务边界明确复用 DTO 的空/零字段及无持久化。
