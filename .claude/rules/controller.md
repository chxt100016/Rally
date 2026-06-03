## controller 规范
1. 项目同时支持app、web、wechat等渠道， 非渠道专属能力统一放在web中并在对应渠道包下创建controller继承web的controller并修改类的mapping为`/{渠道}/{原mapping}`, 例子：`@RequestMapping("/query")` -> `@RequestMapping("/wechat/query")` 
2. app放在`rally-adapter/src/main/java/com/rally/app`
3. web放在`rally-adapter/src/main/java/com/rally/web`
4. wechat放在`rally-adapter/src/main/java/com/rally/wechat`

