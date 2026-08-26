# identity-account.account-login.activity.verify-wechat-identity 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] code 在活动前如何校验，微信配置缺失时返回什么？
  > 流程先拒绝 null、空或纯空白 code，报 AUTH_CODE_REQUIRED；活动检查 code2sessionUrl，缺失时报 WECHAT_LOGIN_FAILED 且不发请求。
  → 已写入触发条件、异常分支、业务动作 A1 与详细流程第 1 步

- [Q2] 微信请求参数和有效响应的最低条件是什么？
  > GET 配置 URL，传 appid、secret、js_code、grant_type=authorization_code；响应必须非空、errcode=0 且 openid 非空，否则 WECHAT_LOGIN_FAILED 并保留微信 errmsg 线索。
  → 已写入业务动作 A2-A3 与详细流程第 2-3 步

- [Q3] 成功身份数据哪些字段必须返回，session_key 是否进入下游？
  > 活动输出必填 openid，以及可空 unionid；微信 session_key 虽被客户端解析，但账户建立与登录结果都不使用、不持久化、不对外返回。
  → 已写入活动契约、详细流程第 4 步与边界情况
