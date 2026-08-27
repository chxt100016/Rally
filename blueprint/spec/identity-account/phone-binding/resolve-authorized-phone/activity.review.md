# identity-account.phone-binding.activity.resolve-authorized-phone 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 动态令牌和微信接口配置如何校验，访问令牌从哪里取得？
  > code 非空白由流程校验；活动还要求 phoneNumberUrl 非空，先通过微信 access token 客户端取得非空访问令牌，再 POST 手机号接口，任一缺失报 WECHAT_PHONE_FAILED。
  → 已写入异常分支、业务动作 A1 与详细流程第 1 步

- [Q2] 手机号请求与有效响应的最低条件是什么？
  > 在 phoneNumberUrl 后带 access_token 查询参数，JSON body 为 code；响应必须非空、errcode=0、phone_info 非空且 phoneNumber 非空，否则 WECHAT_PHONE_FAILED。
  → 已写入业务动作 A2-A3 与详细流程第 2-3 步

- [Q3] 微信返回的其他手机号字段是否进入绑定或对外返回？
  > 活动只输出 phoneNumber 供下游绑定；countryCode、purePhoneNumber 等即使解析也不参与用户更新，响应不向调用方回传手机号详情。
  → 已写入活动契约、详细流程第 4 步与实现提示

- [Q4] 活动是否应透传主线的 30002/30003 分流，而非统一成新的 WECHAT_PHONE_FAILED？
  > 是。沿用 WechatMiniappClient 既有异常：access token 为空为 WECHAT_AUTH_FAILED，其余手机号接口失败为 WECHAT_PHONE_NUMBER_FAILED。
  → 时序图、异常分支与详细流程已明确透传两种既有错误码。
