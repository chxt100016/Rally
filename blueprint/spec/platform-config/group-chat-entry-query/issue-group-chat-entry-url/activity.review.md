# platform-config.group-chat-entry-query.activity.issue-group-chat-entry-url 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 二维码对象 key 是否来自系统配置或用户身份？
  > 都不是；登录仅用于鉴权，始终签名写死的 default/qrcode.jpg，不读取 system.group.qrcode。
  → 已写入业务动作 A1 与详细流程第 1-2 步

- [Q2] 签名使用哪些七牛属性和期限？
  > 用 domain 决定协议并构址、access/secret 签名到当前Unix秒+3600；bucket不参与本次下载地址签名。
  → 已写入业务动作 A2 与详细流程第 3 步

- [Q3] 是否验证对象存在，成功响应是什么形态？
  > 不验证对象存在；直接返回单个签名URL字符串，不返回对象或期限字段，也不记录签发。
  → 已写入活动契约、详细流程第 4 步与边界情况
