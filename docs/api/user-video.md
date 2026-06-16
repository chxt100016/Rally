# 用户视频管理 接口文档（前端）

用户域「视频」能力：上传视频、删除视频、修改视频标题。

## 通用约定

- **统一前缀**：所有接口均带 `/api/rally` 前缀；微信渠道在此基础上多一层 `/wechat`（如 `/api/rally/wechat/user/profile/video/upload`），入参出参完全一致。
- **登录态**：均需登录，请求头携带 `Authorization: {token}`。
- **统一响应包装** `Result<MyProfileDTO>`：

  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `code` | int | `0` 成功；非 0 为业务错误码 |
  | `message` | string | 错误信息，成功时为 `null` |
  | `data` | MyProfileDTO | 更新后的用户档案信息 |

- **相关错误码**：

  | code | message | 说明 |
  | --- | --- | --- |
  | 10001 | 未登录，请先登录 | 缺少/无效登录态 |
  | 20001 | 数据不存在 | 用户档案不存在 |

---

## 1. 上传视频

- **POST** `/api/rally/user/profile/video/upload`
- 追加一条视频到用户视频列表

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 视频 key（存储标识） |
| `title` | string | 否 | 视频标题 |

```bash
curl -X POST 'http://localhost:8080/api/rally/user/profile/video/upload' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{"key":"video_abc123","title":"我的发球练习"}'
```

响应：`data` 为更新后的 `MyProfileDTO`。

```json
{
  "code": 0,
  "message": null,
  "data": {
    "user": { "...": "用户信息" },
    "profile": {
      "videos": [
        { "key": "video_abc123", "title": "我的发球练习" }
      ],
      "...": "其他档案信息"
    }
  }
}
```

---

## 2. 删除视频

- **POST** `/api/rally/user/profile/video/delete`
- 根据 video key 删除指定视频

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 视频 key（存储标识） |

```bash
curl -X POST 'http://localhost:8080/api/rally/user/profile/video/delete' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{"key":"video_abc123"}'
```

响应：`data` 为更新后的 `MyProfileDTO`。

```json
{
  "code": 0,
  "message": null,
  "data": {
    "user": { "...": "用户信息" },
    "profile": {
      "videos": [],
      "...": "其他档案信息"
    }
  }
}
```

---

## 3. 修改视频

- **POST** `/api/rally/user/profile/video/update`
- 根据 video key 修改视频标题

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 视频 key（存储标识） |
| `title` | string | 否 | 视频标题 |

```bash
curl -X POST 'http://localhost:8080/api/rally/user/profile/video/update' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{"key":"video_abc123","title":"新标题"}'
```

响应：`data` 为更新后的 `MyProfileDTO`。

```json
{
  "code": 0,
  "message": null,
  "data": {
    "user": { "...": "用户信息" },
    "profile": {
      "videos": [
        { "key": "video_abc123", "title": "新标题" }
      ],
      "...": "其他档案信息"
    }
  }
}
```

---

## 数据结构

### VideoVO

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `key` | string | 视频 key（存储标识） |
| `title` | string | 视频标题 |

---

## 前端使用流程

### 上传视频
1. 先调用上传接口获取视频 key（七牛云等存储返回的 key）
2. 调 `POST /user/profile/video/upload` 传入 `key` 和可选的 `title`
3. 接口返回更新后的完整用户档案，更新本地视频列表

### 删除视频
1. 用户确认删除后，调 `POST /user/profile/video/delete` 传入视频 `key`
2. 接口返回更新后的完整用户档案，更新本地视频列表

### 修改视频标题
1. 用户编辑标题后，调 `POST /user/profile/video/update` 传入视频 `key` 和新 `title`
2. 接口返回更新后的完整用户档案，更新本地视频列表
