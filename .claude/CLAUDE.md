# CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与运行
```bash
# 构建整个项目（跳过测试）
mvn clean package -DskipTests
```
## COLA 架构分层
项目严格遵循 COLA 5.0 架构，
- 模块依赖方向：
  - `start（聚合启动，@SpringBootApplication 扫包 com.rally） → rally-adapter → rally-app`
  - `rally-app → rally-infrastructure`
  - `rally-infrastructure → rally-domain`
- `rally-adapter`:REST Controller（`com.rally.web`）+ 定时 Job（`com.rally.job`）
- `rally-app`: 应用服务，编排业务流程，不含领域规则
- `rally-domain`:领域模型（VO/Data/Enum）、Gateway 接口、工具类
- `rally-infrastructure`:Gateway 实现、MyBatis-Plus ORM、外部 API Client、FastJson 配置
- `start`: Spring Boot 入口，`@MapperScan("com.rally.db")` |


## 关键架构规则
- 所有请求接口前缀在配置文件中配置了`/api/rally`
- 方法的返回值不要用optional
- **Domain 层不能直接调用 Infrastructure**。跨层访问必须通过 Gateway 模式：
  1. 在 `rally-domain/src/main/java/com/rally/domain/tennis/gateway/` 定义接口
  2. 在 `rally-infrastructure/src/main/java/com/rally/db/tennis/repository/` 用 `@Component` 实现该接口
- FastJson2 作为 HTTP 消息转换器（`rally-infrastructure/config/FastJsonConfigClass.java`）
- 枚举的name要使用大写
- 对外返回的对象都以DTO结尾