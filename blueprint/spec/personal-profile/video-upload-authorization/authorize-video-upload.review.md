# personal-profile.video-upload-authorization.flow.authorize-video-upload 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 视频数量何时统计，未登记文件、多次并发申请和无档案用户如何影响授权？
  > 只在签发时统计 profile.videos 已登记条目；七牛已有未登记文件不计，令牌不占名额，多次/并发申请都可通过同一数量快照。profile 不存在、videos null 或空列表均继续签发，不建档。
  → 已在触发、详细流程、异常说明和服务边界明确只数已登记项、授权不占位及无档案继续。

- [Q2] 数量与大小配置为空、非法或负数时如何判断和签发？
  > 缺配置用枚举默认。非法整数统一为 0；maxCount=0 时非空列表 size>=0 拒绝，但空/无档案因跳过判断仍签发。负 maxCount 同样只对非空列表拒绝。maxSize 非法为0，负值换算成负 fsizeLimit 后交给七牛。
  → 已在详细流程和异常说明明确缺省、非法与负数配置的分支。

- [Q3] 最终七牛 scope 是否真正限制在 videos/{userId}/ 前缀，令牌期限是 600 还是 3600 秒？
  > 不真正限制。policy 先写 bucket:videos/userId/ 与 isPrefixalScope=1，但 Auth.uploadToken(bucket,null,3600,policy) 最终把 scope 设为 bucket、deadline 设为当前时间+3600；令牌可用范围按整个桶、一小时。
  → 已在契约、详细流程和技术线索明确最终整桶 scope 与 3600 秒期限。

- [Q4] 返回的 60 秒时长是否进入上传策略，是否限制媒体类型、扩展名和内容？
  > 不进入。maxDurationSec 固定返回60，不读取 USER_VIDEO_MAX_SECOND，也没有七牛时长策略；令牌不限制 MIME、文件扩展名、视频编码或内容，只保留 fsizeLimit。
  → 已在详细流程、契约和服务边界明确 60 秒仅展示且无媒体内容限制。

- [Q5] 成功响应的 key、keyPrefix、resourceUrl 等字段分别是什么？
  > 返回 uploadToken、keyPrefix=videos/{userId}/、maxSizeMb、maxDurationSec=60、uploadHost=https://up-z0.qiniup.com；固定前缀模式未设置 key 与 resourceUrl，二者为 null。
  → 已在契约和技术线索明确各响应字段及 key/resourceUrl 为空。
