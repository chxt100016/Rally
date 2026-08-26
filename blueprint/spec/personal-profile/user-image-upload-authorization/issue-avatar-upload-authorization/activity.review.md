# personal-profile.user-image-upload-authorization.activity.issue-avatar-upload-authorization 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 头像 key、扩展名与同秒重复请求如何处理？
  > 固定生成 avatar/{userId}_{yyyyMMddHHmmss}.jpeg，同一用户同秒请求得到同一精确 key。
  → 已写入业务动作 A2、详细流程第 3 步与边界情况

- [Q2] 大小、scope 和令牌期限是什么？
  > 大小读配置默认5MB、非法为0；scope 精确到 key，策略先写600秒 deadline，但 SDK 以3600秒签发，当前实际一小时。
  → 已写入业务动作 A1/A3、详细流程第 2、4 步

- [Q3] 是否校验账户、文件并保存授权或删除旧头像？
  > 都不做；只用登录 userId 签发，预览地址不证明文件存在，授权和 key 不落库。
  → 已写入活动契约、详细流程第 1、5 步与边界情况
