## controller 接口 规范
1. 项目同时支持app、web、wechat等渠道， 非渠道专属能力统一放在web中并在对应渠道包下创建controller
    - app放在`rally-adapter/src/main/java/com/rally/app`
    - web放在`rally-adapter/src/main/java/com/rally/web`
    - wechat放在`rally-adapter/src/main/java/com/rally/wechat`

2. 如果为渠道专属能力，直接在对应包创建, 比如微信登陆能力是微信专属
3. 如果为非渠道专属的通用能力，在web下创建通用接口，并在对应渠道包下创建实现类, 比如用户信息查询
   - 类名为渠道名+原名，例：`WechatUserQueryController` 
   - 重写类的mapping为`/{渠道}/{原mapping}`, 例子：`@RequestMapping("/query")` -> `@RequestMapping("/wechat/query")
   - 如果没有特殊需求，直接空实现不要重写方法。
   - 如果有渠道专属能力就重写方法或者新增方法。


