# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与运行

```bash
# 构建整个项目（跳过测试）
mvn clean package -DskipTests

# 运行（启动模块）
cd start && mvn spring-boot:run

# 运行单个测试
mvn test -pl <module> -Dtest=TestClassName#methodName
```

应用启动后地址：`http://localhost:9482/api/rally`

默认激活 `dev` profile，MySQL 连接 `localhost:3306/fan`（用户名/密码均为 `root`）。

## COLA 架构分层

项目严格遵循 COLA 5.0 架构，模块依赖方向：

```
rally-adapter → rally-app → rally-domain
rally-infrastructure → rally-domain
start（聚合启动，@SpringBootApplication 扫包 com.rally）
```

| 模块 | 职责 |
|---|---|
| `rally-adapter` | REST Controller（`com.rally.web`）+ 定时 Job（`com.rally.job`） |
| `rally-app` | 应用服务，编排业务流程，不含领域规则 |
| `rally-domain` | 领域模型（VO/Data/Enum）、Gateway 接口、工具类；**不依赖 Spring** |
| `rally-infrastructure` | Gateway 实现、MyBatis-Plus ORM、外部 API Client、FastJson 配置 |
| `start` | Spring Boot 入口，`@MapperScan("com.rally.db")` |

## 关键架构规则

**HTTP 请求必须使用项目的 Http 工具类**，位于 `rally-domain/src/main/java/com/rally/domain/utils/Http.java`，链式调用示例：
```java
Http.uri(url).param("key", "val").header("Authorization", token)
    .doGet().result(SomeClass.class);
```

**Domain 层不能直接调用 Infrastructure**。跨层访问必须通过 Gateway 模式：
1. 在 `rally-domain/src/main/java/com/rally/domain/tennis/gateway/` 定义接口
2. 在 `rally-infrastructure/src/main/java/com/rally/db/tennis/gateway/` 用 `@Component` 实现该接口

## 数据库与 ORM

- **MyBatis-Plus**：Mapper 继承 `BaseMapper<PO>`，Service 继承 `ServiceImpl`
- PO 实体用 `@TableName` 注解，放在 `com.rally.db.tennis.entity`
- Repository 门面层（`com.rally.db.tennis.repository`）封装 Service，供 app 层调用
- 无自定义 XML Mapper，复杂查询用 `LambdaQueryWrapper`
- FastJson2 作为 HTTP 消息转换器（`rally-infrastructure/config/FastJsonConfigClass.java`）

## 对象映射

- 对象映射使用 MapStruct（`org.mapstruct`） 的单例模式:`MatchAppConvertMapper INSTANCE = Mappers.getMapper(MatchAppConvertMapper.class);`
- 当只需要原类型的单个的属性的时候使用named注解: `@Named("groupToId")`

## 外部 API

| Client | 外部地址 |
|---|---|
| `TennisTvClient` | `https://api.tennistv.com/tennis/v1` |
| `WtaClient` | `https://api.wtatennis.com/tennis/tournaments` |

两个 Client 均在 `rally-infrastructure/client/` 下，直接使用 `Http` 工具类调用。

## 定时任务

定时 Job 在 `rally-adapter/src/main/java/com/rally/job/TennisCollectJob.java`。任务开关通过 `job.tennis.enabled` 控制（prod 为 `true`，dev 默认不启用），cron 表达式在 `application-prod.yml` 中配置。
