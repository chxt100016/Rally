# platform-config.splash-cover-query.flow.query-splash-cover 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 匿名、缺失或无效登录令牌如何处理，身份是否影响封面？
  > 接口 OptionalAuth；缺失、格式错误或无效令牌均匿名继续。身份不参与 key 选择，所有访问者使用同一当前配置。
  → 接口契约、详细流程第 1 步与匿名降级分支

- [Q2] 封面 key 的覆盖值、停用/缺失和默认值优先级是什么？
  > SystemConfig 取已启用 global 缓存覆盖值；无启用值（含记录不存在/停用）回退 SystemConfigKey 默认 default/splash-cover-20260821.jpg。
  → 详细流程第 2 步与默认封面分支

- [Q3] 已启用配置为空白时是否回退默认值，成功响应实际包含什么？
  > 已启用覆盖值只要被缓存就优先；若为空白，buildSignedUrl 直接返回 null，不回退默认。成功 Result.data 是 URL 字符串或 null，不带 key、来源或 expires。
  → 接口契约、详细流程第 3、5 步与空结果分支

- [Q4] 七牛签名如何确定域名、有效期和失败口径，是否验证对象存在？
  > domain 前缀决定 HTTP/HTTPS，accessKey/secretKey 签名，deadline=当前 Unix 秒+3600，bucket 不参与。配置/签名异常走全局 OPERATION_FAILED 与系统异常提示；不验证对象存在，所以坏 key 也可能成功返回 URL。
  → 详细流程第 4-5 步、OPERATION_FAILED 分支与服务边界
