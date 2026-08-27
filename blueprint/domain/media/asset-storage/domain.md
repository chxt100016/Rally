---
id: "@media.asset-storage"
kind: service
reads: []
---

## 概要

封装 main 现有的七牛令牌、签名地址与删除调用。

## 职责

| 范围 | 内容 |
|---|---|
| 做什么 | 按调用方构造的原始策略与 SDK 参数签发上传令牌；生成限时读取 URL；执行对象删除并容忍七牛文件不存在 |
| 不做什么 | 不读取或修改业务表；不判断用户是否拥有资源；不验证媒体内容、对象是否存在或是否已被业务记录引用；不参与数据库事务或恢复已删除对象 |
| 为什么不是聚合 | 授权、签名和删除在任何业务聚合被创建前都可调用，服务自身不保存需要事务维护的领域状态 |
| 前置校验边界 | 服务不附加资源键、命名空间、容量或媒体内容校验；调用活动按 main 原样给出 key、策略和 SDK 参数 |

## 契约

### 输入

| 操作 | 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|---|
| 签发上传授权 | policyScopeMode | Enum | 是 | `EXACT_KEY` 或 `KEY_PREFIX`，用于构造初始 policy |
| 签发上传授权 | policyResourceScope | String | 是 | 调用方原样构造的精确 key 或前缀；不做路径安全或归属校验 |
| 签发上传授权 | sdkKey | String? | 否 | 头像/用户图片传精确 key；视频前缀授权传 null，允许 SDK 以桶级 scope 重建最终策略 |
| 签发上传授权 | maxBytes | Long | 是 | 原样写入 `fsizeLimit`，允许 0 或负值 |
| 签发上传授权 | policyDeadlineSeconds | Long | 是 | 初始策略 deadline 相对秒数，当前调用固定 600 |
| 签发上传授权 | tokenTtlSeconds | Long | 是 | 传给 SDK 的令牌 TTL，当前调用固定 3600，可与策略 deadline 不同 |
| 签发读取 URL | resourceKey | String | 否 | 原始对象 key；空白时返回 null，其他值不做路径校验 |
| 签发读取 URL | expiresInSeconds | Long | 是 | 当前调用固定 3600 |
| 删除对象 | resourceKey | String | 否 | 调用方传入的原始 key；不在服务内复核所有权或格式 |

### 输出

| 操作 | 字段 | 类型 | 说明 |
|---|---|---|---|
| 签发上传授权 | outcome | Enum | `AUTHORIZED` 或 `REJECTED` |
| 签发上传授权 | uploadToken | String? | 授权成功时返回，scope 与输入模式严格一致 |
| 签发上传授权 | policyDeadlineAt | Instant? | 初始策略写入的截止时间 |
| 签发上传授权 | tokenExpiresAt | Instant? | SDK 令牌 TTL 对应时间；可与策略截止时间不同 |
| 签发读取 URL | outcome | Enum | `SIGNED` 或 `REJECTED` |
| 签发读取 URL | signedUrl | String? | 签名成功时返回；不代表对象存在 |
| 签发读取 URL | expiresAt | Instant? | URL 实际截止时间 |
| 删除对象 | outcome | Enum | `DELETED`、`ALREADY_ABSENT` 或 `FAILED` |

## 规则

R1 key、type、扩展名、前缀和删除参数按调用方原样交付，不拒绝空白、前导 `/`、反斜杠、`.`、`..`、跨目录或非本人路径；各活动的请求层校验和自然异常保持各自语义。
R2 头像和用户图片把精确 key 同时写入 policy scope 与 SDK key；视频先写前缀 scope/`isPrefixalScope=1`，随后以 `sdkKey=null` 调用 SDK，允许 SDK 把最终 scope 重建为整个桶。不得把这一现状强化成真正的前缀隔离。
R3 `maxBytes` 原样写入 `fsizeLimit`；配置解析为 0 或负数时仍继续签发，不返回领域拒绝。
R4 上传 policy deadline 与 token TTL 是两个独立参数。当前 main 先写 600 秒 deadline，再以 3600 秒 TTL 调用 SDK；最终 SDK 是否覆盖 deadline/scope 按供应商实现保留。
R5 临时读取 URL 对空白 key 返回 null；非空白 key 按 3600 秒生成签名，不探测对象存在、不校验其格式或归属。
R6 删除把原始 key 交给七牛。删除成功完成；七牛 612（文件不存在）同样完成；其他异常向调用活动传播。外部删除不参与数据库事务或补偿。

## 边界情况

- 同一秒为同一精确键签发多次：允许返回多个有效令牌；上传时可能互相覆盖。
- 视频返回的 `videos/{userId}/` 是建议前缀；最终令牌因 `sdkKey=null` 可能是桶级 scope。
- `type` 或头像扩展名含空白、`/`、`..`、反斜杠或超长内容：原样进入 key 并尝试签发。
- `maxBytes` 为 0 或负数：仍写入策略并尝试签发，返回的 MB 限制保留配置解析结果。
- policy deadline 为 600 秒而 token TTL 为 3600 秒：两者并存，不能合并成单一过期参数。
- 对不存在对象签发读取 URL：可以返回 `SIGNED`；实际读取可能由对象存储返回不存在。
- 重复删除或对象原本不存在：返回 `ALREADY_ABSENT`，不作为业务失败。
- 外部删除成功后数据库事务失败：对象不会自动恢复；调用方必须接受失效引用风险或采用提交后删除/补偿任务。
- 外部凭据、桶或域名无效：返回相应操作的失败结论，不持久化授权结果。

## 实现提示

七牛适配器按 main 的 `StringMap` 顺序构造 `scope`、可选 `isPrefixalScope`、`fsizeLimit` 和 600 秒 `deadline`，再调用 `uploadToken(bucket, sdkKey, 3600, policy)`；不要为了安全性阻止 SDK 重建最终 scope/deadline。签名地址复用 `QiniuConfiguration.buildSignedUrl` 的空白返回 null、3600 秒和原始 key 行为。删除复用 `QiniuClient.deleteFile` 的 612 容错，其余异常原样传播。
