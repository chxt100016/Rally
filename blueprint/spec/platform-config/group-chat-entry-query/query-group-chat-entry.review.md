# platform-config.group-chat-entry-query.flow.query-group-chat-entry 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 接口是否允许匿名访问，用户身份是否影响返回的二维码？
  > GET /system/qrcode 未标 OptionalAuth，必须通过普通 Bearer 鉴权；用户身份不参与素材选择，所有已登录用户取得同一对象的新签名地址。
  → 接口契约、详细流程第 1 步及鉴权异常分支

- [Q2] 二维码对象 key 来自系统配置还是固定值，查询前是否验证对象存在？
  > 固定使用 default/qrcode.jpg，不读取 system.group.qrcode；签名生成不向七牛查询对象，因此对象不存在也可能返回看似有效但无法打开的 URL。
  → 详细流程第 2 步、异常分支说明与服务边界

- [Q3] 签名 URL 如何确定协议、域名和有效期，bucket 是否参与？
  > domain.startsWith("https") 决定 HTTPS，否则 HTTP，并去掉 http(s) 前缀；deadline=当前 Unix 秒+3600，accessKey/secretKey 参与签名，bucket 不参与下载 URL 构造。
  → 详细流程第 3-4 步与技术线索

- [Q4] 成功响应的实际结构是什么，七牛配置缺失或签名失败如何处理？
  > Result 的 data 是一个 URL 字符串，不是 qrcode 对象或 base64，也无单独 expires 字段。domain/凭据缺失、格式问题或七牛 QiniuException 均无业务降级，最终按系统异常失败。
  → 接口契约、详细流程第 5 步与 SYSTEM_ERROR 分支
