# 用户登录注册系统 · 技术实施规格

**日期：** 2026-05-27
**关联设计：** [auth-design.md](./auth-design.md)
**阶段：** MVP
**架构：** COLA 5.0（rally-adapter / rally-app / rally-domain / rally-infrastructure）

---

## 一、技术栈与依赖

| 能力 | 选型 | 说明 |
|------|------|------|
| 认证协议 | JWT（HS256） | 无状态 token，签名密钥从环境变量注入 |
| JWT 库 | `io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson` 0.12.x | 新增依赖，加在 `rally-infrastructure/pom.xml` |
| 微信登录 | `code2Session` 接口 | 沿用项目内 `com.rally.domain.utils.Http` 工具类调用，不引入第三方 SDK，与 `TennisTvClient`/`WtaClient` 风格一致 |
| ORM | MyBatis-Plus | `BaseMapper` + Repository 门面，沿用现有模式 |
| 对象映射 | MapStruct | `Mappers.getMapper(...)` 单例模式 |
| 统一返回 | `com.rally.domain.tennis.model.Result<T>` | 现有结构 `{ code, data }`，本期直接复用；后续可上提到 common 包 |
| ID 生成 | MyBatis-Plus 雪花算法 | `@TableId(type = IdType.ASSIGN_ID)`，DB 列存 `VARCHAR(32)` 字符串形式（避免前端 JS 精度丢失） |

---

## 二、模块与包结构

下表列出所有需新建的文件落点。包路径严格遵循 `.claude/CLAUDE.md` 中的 COLA 分层规则。

### rally-domain（领域层，不依赖 Spring）

| 路径 | 类型 | 说明 |
|------|------|------|
| `com.rally.domain.user.model.UserVO` | VO | 对外暴露的用户视图 |
| `com.rally.domain.user.model.UserData` | Data | 领域内用户数据 |
| `com.rally.domain.user.gateway.UserGateway` | 接口 | 用户读写 |
| `com.rally.domain.auth.model.LoginResultVO` | VO | 登录响应：`token` / `userId` / `isNewUser` |
| `com.rally.domain.auth.model.WechatLoginCmd` | DTO | 登录入参：`code` |
| `com.rally.domain.auth.model.WechatSession` | Data | code2Session 返回：`openid` / `unionid` / `sessionKey` |
| `com.rally.domain.auth.model.TokenPayload` | Data | JWT 解析结果：`userId` |
| `com.rally.domain.auth.gateway.AccountGateway` | 接口 | accounts 表读写 |
| `com.rally.domain.auth.gateway.WechatGateway` | 接口 | 微信平台调用（code2Session） |
| `com.rally.domain.auth.gateway.TokenGateway` | 接口 | JWT 签发/校验 |
| `com.rally.domain.auth.enums.ChannelEnum` | enum | `WECHAT_MINIAPP` / `PHONE`（PHONE 仅占位，本期不实现） |
| `com.rally.domain.user.enums.GenderEnum` | enum | `MALE` / `FEMALE` / `UNDISCLOSED` |

### rally-app（应用层）

| 路径 | 说明 |
|------|------|
| `com.rally.auth.AuthAppService` | 编排：调用 WechatGateway → 查 AccountGateway → 命中/新建 User → 调用 TokenGateway 签发 |
| `com.rally.auth.convert.AuthConvertMapper` | MapStruct，UserData ↔ UserVO、UserData ↔ LoginResultVO |
| `com.rally.user.UserQueryService` | 查询当前用户（GET /wechat/user/me） |

### rally-infrastructure（基础设施层）

| 路径 | 说明 |
|------|------|
| `com.rally.db.user.entity.UserPO` | `@TableName("users")` |
| `com.rally.db.user.mapper.UserMapper` | `extends BaseMapper<UserPO>` |
| `com.rally.db.user.service.UserService` / `UserServiceImpl` | MyBatis-Plus Service |
| `com.rally.db.user.repository.UserRepository` | 门面，封装常用查询 |
| `com.rally.db.user.gateway.UserGatewayImpl` | `@Component`，实现 `UserGateway` |
| `com.rally.db.auth.entity.AccountPO` | `@TableName("accounts")` |
| `com.rally.db.auth.mapper.AccountMapper` | `extends BaseMapper<AccountPO>` |
| `com.rally.db.auth.service.AccountService` / `AccountServiceImpl` | MyBatis-Plus Service |
| `com.rally.db.auth.repository.AccountRepository` | 门面，提供 `findByChannelAndIdentifier` |
| `com.rally.db.auth.gateway.AccountGatewayImpl` | `@Component`，实现 `AccountGateway` |
| `com.rally.db.auth.gateway.TokenGatewayImpl` | `@Component`，实现 `TokenGateway`（jjwt） |
| `com.rally.client.wechat.WechatMiniappClient` | `@Component`，实现 `WechatGateway`，调用 `Http.uri(...)` |
| `com.rally.config.WechatMiniappProperties` | `@ConfigurationProperties("wechat.miniapp")` |
| `com.rally.config.AuthJwtProperties` | `@ConfigurationProperties("auth.jwt")` |

### rally-adapter（接入层）

| 路径 | 说明 |
|------|------|
| `com.rally.wechat.WechatAuthController` | `POST /wechat/auth/login` |
| `com.rally.wechat.WechatUserController` | `GET /wechat/user/me`（需 token） |
| `com.rally.web.auth.AuthInterceptor` | `HandlerInterceptor`，解析 `Authorization: Bearer <token>` |
| `com.rally.web.auth.CurrentUser` | 自定义注解 `@CurrentUser` |
| `com.rally.web.auth.CurrentUserResolver` | `HandlerMethodArgumentResolver`，把 userId 注入到方法参数 |
| `com.rally.web.auth.UserContext` | `ThreadLocal<String>` 持有当前 userId，请求结束清理 |
| `com.rally.web.config.WebMvcConfig` | 注册拦截器与参数解析器，配置白名单 |

---

## 三、数据库 DDL

```sql
-- 用户核心表
CREATE TABLE `users` (
  `user_id`     VARCHAR(32) NOT NULL COMMENT '系统唯一 ID（雪花算法字符串形式）',
  `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  `avatar_url`  VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
  `gender`      ENUM('male','female','undisclosed') NOT NULL DEFAULT 'undisclosed' COMMENT '性别',
  `birthday`    DATE         DEFAULT NULL COMMENT '生日，用于年龄段筛选，不直接展示',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号（MVP 不收集，列保留供后续手机号注册使用）',
  `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱（MVP 不收集，列保留）',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户核心表';

-- 渠道认证表
CREATE TABLE `accounts` (
  `account_id`     VARCHAR(32) NOT NULL COMMENT '账号唯一 ID',
  `user_id`        VARCHAR(32) NOT NULL COMMENT '关联 users.user_id',
  `channel`        ENUM('phone','wechat_miniapp') NOT NULL COMMENT '渠道类型',
  `identifier`     VARCHAR(128) NOT NULL COMMENT '渠道唯一标识：手机号 或 wechat_openid',
  `credential`     VARCHAR(256) DEFAULT NULL COMMENT '凭证：密码哈希；微信渠道保持 NULL',
  `wechat_unionid` VARCHAR(128) DEFAULT NULL COMMENT '微信跨端识别，仅微信渠道填写',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_channel_identifier` (`channel`, `identifier`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道认证表';
```

> 字段口径修正：design 中 `users.邮箱` 改为 `email`（design 文档名称为中文，落库以本 spec 为准）。

---

## 四、API 契约

请求基础路径：`http://localhost:9482/api/rally`（参见 `.claude/CLAUDE.md`）。

### 4.1 微信一键登录

- **路由**：`POST /wechat/auth/login`
- **鉴权**：免登录（白名单）
- **入参**（JSON Body）：

```json
{ "code": "0a1b2c3d4e5f..." }
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | string | 是 | 小程序 `wx.login()` 返回的临时登录凭证 |

- **响应**：`Result<LoginResultVO>`

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "1234567890123456789",
    "isNewUser": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | string | JWT，前端后续放 `Authorization: Bearer <token>` |
| `userId` | string | 用户 ID |
| `isNewUser` | boolean | 是否本次登录新建账号；前端据此决定是否拉起 Onboarding 弹窗，详见 [date-design.md](../date/date-design.md) |

### 4.2 获取当前用户

- **路由**：`GET /wechat/user/me`
- **鉴权**：需 token
- **响应**：`Result<UserVO>`

```json
{
  "code": 0,
  "data": {
    "userId": "1234567890123456789",
    "nickname": "张三",
    "avatarUrl": "https://...",
    "gender": "male",
    "birthday": "1995-08-12"
  }
}
```

> MVP 仅这两个接口。Onboarding 阶段的资料更新（昵称/头像/性别/生日/NTRP/视频）属于 date 模块范畴，不在本 spec。

---

## 五、核心时序

### 5.1 微信一键登录

```
小程序                后端 Controller         AuthAppService          WechatGateway        AccountGateway        UserGateway          TokenGateway
  │  wx.login()             │                       │                        │                    │                    │                    │
  │ ──────────────────────▶ │                       │                        │                    │                    │                    │
  │ POST /wechat/auth/login │                       │                        │                    │                    │                    │
  │       (code)            │ ──── login(code) ───▶ │                        │                    │                    │                    │
  │                         │                       │ ── code2Session(code)─▶│                    │                    │                    │
  │                         │                       │ ◀──── WechatSession ───│                    │                    │                    │
  │                         │                       │ ── findByChannelAndIdentifier(WECHAT, openid)──────────────────▶ │                    │
  │                         │                       │ ◀── Optional<Account> ─│                    │                    │                    │
  │                         │                       │  命中：取 userId                                                                         │
  │                         │                       │  未命中：createUser → createAccount(WECHAT, openid, unionid)                            │
  │                         │                       │ ─────────────── issue(userId) ──────────────────────────────────────────────────────────▶ │
  │                         │                       │ ◀───────────────── token ──────────────────────────────────────────────────────────────── │
  │ ◀── LoginResultVO ───── │ ◀── LoginResultVO ─── │                        │                    │                    │                    │
```

要点：
- `isNewUser = true` 当且仅当本次调用新建了 user + account 记录；前端凭此决定是否拉起 Onboarding
- `WechatSession.unionid` 写入 `accounts.wechat_unionid`；`sessionKey` 不入库（后续如需解密用户信息再考虑短期缓存）

### 5.2 Token 校验

```
请求 ─▶ AuthInterceptor.preHandle
          ├─ 路径在白名单（/wechat/auth/login、/actuator/health） → 放行
          ├─ 缺 Authorization 头 → Result(10002)
          ├─ TokenGateway.verify(token) 失败 → Result(10003)
          └─ 成功 → UserContext.set(userId) → 放行
              │
              ▼
        Controller 方法签名包含 @CurrentUser String userId
              │
              ▼
        CurrentUserResolver 从 UserContext 取出注入
              │
              ▼
        afterCompletion: UserContext.clear()
```

---

## 六、JWT 设计

| 项 | 值 |
|----|----|
| 算法 | HS256 |
| Header | `{ "alg": "HS256", "typ": "JWT" }` |
| Claims | `sub = userId`、`iat`、`exp` |
| 有效期 | 7 天（`auth.jwt.expire-days` 配置） |
| 签名密钥 | 从 `auth.jwt.secret` 注入，**禁止硬编码**，长度 ≥ 32 字节 |
| 续期 | MVP 不做 refresh token，过期重新走 `wx.login` |
| 撤销 | MVP 不做黑名单（无服务端状态） |

`TokenGateway` 接口签名建议：

```java
public interface TokenGateway {
    /** 签发 token */
    String issue(String userId);

    /** 校验并解析；失败返回 Optional.empty() */
    Optional<TokenPayload> verify(String token);
}
```

---

## 七、配置项清单

新增到 `start/src/main/resources/application-wechat.yml`（与现有 datasource、job 配置同文件）：

```yaml
wechat:
  miniapp:
    appid: ${WECHAT_APPID}
    secret: ${WECHAT_SECRET}
    code2session-url: https://api.weixin.qq.com/sns/jscode2session

auth:
  jwt:
    secret: ${JWT_SECRET}
    expire-days: 7
```

`application-dev.yml` 可提供本地默认值（用占位 secret，便于调试）；`application-prod.yml` 必须仅引用环境变量，**不写明文密钥**。

---

## 八、错误码

沿用 `Result.code`：`0` 成功，`> 0` 业务错误。HTTP 状态码统一 200，不抛 4xx。

| code | 含义 | 触发场景 |
|------|------|----------|
| 0 | 成功 | — |
| 10001 | 微信 code 无效 | code2Session 返回 errcode 非 0，或 openid 为空 |
| 10002 | token 缺失 | Authorization 头缺失或格式错误 |
| 10003 | token 失效或过期 | 验签失败、过期、claims 不合法 |
| 10004 | 用户不存在 | token 合法但 userId 在 users 表中找不到（理论上极少出现） |

---

## 九、安全

- **JWT secret**：`auth.jwt.secret` 必须从环境变量读取，长度 ≥ 32 字节；`application-prod.yml` 中以 `${JWT_SECRET}` 占位
- **微信 secret**：`wechat.miniapp.secret` 同上
- **凭证存储**：微信渠道下 `accounts.credential` 始终为 `NULL`（不存 `access_token`，与 design 第一节一致）
- **拦截器白名单**：仅 `POST /wechat/auth/login` 和 Spring Boot Actuator 健康检查接口放行，其余 `/wechat/**` 全部要求 token
- **日志脱敏**：日志中打印的 token 必须截断（仅保留前 8 位 + `...`），openid 同理
- **HTTPS**：生产环境网关强制 HTTPS，本规格不做应用层强制

---

## 十、MVP 边界（与 design 一致）

**包含：**
- 微信小程序一键登录（`wx.login` → 后端 `code2Session`）
- `accounts` 表多渠道结构（含 `phone` 占位、`unionid` 列）
- JWT token 签发与校验

**不包含（后续迭代，本期不实现，但表结构预留）：**
- 手机号 + 验证码注册（`accounts.channel = phone` 路径）
- 微信与手机号账号合并（基于 `wechat_unionid` 的去重逻辑）
- 第三方社交平台登录（QQ、微博等）
- 密码登录（`accounts.credential` 哈希策略）
- 短信通道选择
- `users.phone` / `users.email` 信息收集接口

**预留设计：**
- `accounts.channel` 已建为 ENUM 包含 `phone`，后续接入手机号无需 DDL 变更
- `users.phone` / `users.email` 列保留为 `NULL`，后续接入手机号注册时直接 `UPDATE`
- `accounts.credential` 列保留，后续密码登录直接写哈希
- `accounts.wechat_unionid` 列保留，后续做 unionid 合并时建索引即可

---

## 十一、遗留事项

1. `Result<T>` 当前位于 `com.rally.domain.tennis.model`，建议后续上提到 `com.rally.domain.common.model`，避免新模块依赖 tennis 子包；本期沿用，不做迁移
2. 全局异常处理（`@ControllerAdvice`）项目当前缺失，本 spec 暂不引入；Controller 内部捕获业务异常后包装为 `Result(code, null)` 返回
3. 参数校验（`@Valid`/`@Validated`）项目当前缺失，本 spec 仅在 `WechatLoginCmd.code` 字段上做空判断（手写），不引入 validation starter
