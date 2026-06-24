# CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与运行
```bash
# 构建整个项目（跳过测试）
mvn clean package -DskipTests
```

## 关键架构规则
- 所有请求接口前缀在配置文件中配置了`/api/rally`
- 任何方法的返回值都不应该用optional
- FastJson2 作为 HTTP 消息转换器（`rally-infrastructure/config/FastJsonConfigClass.java`）
- 枚举的name要使用大写
- 对外返回的对象都以DTO结尾
- 永远不要给方法参数换行，除了链式调用外，一行代码不要换行，不要因为方法参数过多在调用方法的时候换行。
- 雪花id使用场景，比如bizId的生成统一用mybatis-plus的IdWorker.getIdStr()
- 获取领域对象，不要调用gateway 而是用对应的领域service
- 异常码`com.rally.domain.auth.enums.BizErrorCode`
- 业务校验场景使用工具类`com.rally.domain.utils.Assert`, 
  - 不如果能力不够可以拓展实现 
  - 使用例子`Assert.notNull(data, BizErrorCode.MEETUP_NOT_FOUND)`
- 接口出入参数转换放在app层并且使用mapstruct 
- app层不要try catch，将这个放在领域层的业务操作里
- 通过api对外返回的对象创建专门DTO对象， 通过api接收的参数以Cmd结尾
- 获取用户Id通过`UserContext.get()` 方法在app层获取， 