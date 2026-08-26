# identity-account.account-login.activity.issue-login-credential 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] JWT 的主体、签发时间、有效期和签名配置是什么？
  > subject=userId，issuedAt=当前时间，expiration=当前时间+auth.jwt.expireDays，使用 auth.jwt.secret 构造 HMAC 密钥并签名；不写会话表。
  → 已写入业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] 既有用户与新用户每次登录是否复用 token，needCompleteInfo 如何决定？
  > 每次都重新签发 JWT，不复用旧 token，也不主动吊销旧 token；needCompleteInfo 当前严格等于 isNewUser，不检查用户资料实际完整度。
  → 已写入活动契约、业务动作 A3、详细流程第 3-4 步与边界情况

- [Q3] 签名配置无效或签发失败时如何处理，成功返回哪些字段？
  > 密钥为空、长度不足或其他签发异常按 SYSTEM_ERROR；成功返回 token、userId、isNewUser、needCompleteInfo，日志和响应不额外暴露密钥或微信身份。
  → 已写入活动契约、异常分支、边界情况与实现提示
