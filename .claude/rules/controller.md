## controller 接口 规范
1. 通用业务能力统一放在`rally-adapter/src/main/java/com/rally/web`，规范接口不含渠道前缀。
2. 客户端通过`X-Client-Channel`传递渠道，当前支持`WECHAT_MINIAPP`、`WEB`、`APP`。
3. 只有直接依赖渠道 SDK、渠道身份体系或渠道回调协议的能力才放在渠道包并使用渠道路径，例如微信登录、微信手机号和微信支付回调放在`rally-adapter/src/main/java/com/rally/wechat`。
4. 禁止为了增加渠道路径而创建空继承 Controller；旧渠道路径需要兼容时，在通用 Controller 上使用双路径映射，渠道差异通过渠道上下文和策略实现。
5. 每当新增api后都要在完成后提供新api的curl。
6. RequestParam 一定要带上参数名字`@RequestParam("key")`。
7. 使用`spring-boot-starter-validation`校验入参，而不是自己写if判断。
8. controller不要捕获异常。
