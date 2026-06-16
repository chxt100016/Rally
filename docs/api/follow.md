# 关注功能 接口文档（前端）

用户域「关注」能力：关注 / 取消关注、关注列表、被关注（粉丝）列表，以及球员主页新增的关注统计。

## 通用约定

- **统一前缀**：所有接口均带 `/api/rally` 前缀；微信渠道在此基础上多一层 `/wechat`（如 `/api/rally/wechat/user/follow`），入参出参完全一致。
- **登录态**：均需登录，请求头携带 `Authorization: {token}`。
- **统一响应包装** `Result<T>`：

  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `code` | int | `0` 成功；非 0 为业务错误码 |
  | `message` | string | 错误信息，成功时为 `null` |
  | `data` | T | 业务数据 |

- **分页包装** `PageDTO<T>`（游标分页）：

  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `list` | T[] | 数据列表 |
  | `total` | long | 关注列表场景固定为 `null`（不计总数，用 COUNT 接口/主页 stats 取数） |
  | `hasMore` | boolean | 是否还有下一页 |

- **相关错误码**：

  | code | message | 说明 |
  | --- | --- | --- |
  | 40010 | 不能关注自己 | targetUserId 等于当前用户时 |
  | 10001 | 未登录，请先登录 | 缺少/无效登录态 |

---

## 1. 关注用户

- **POST** `/api/rally/user/follow`
- **幂等**：已关注再次调用直接成功，不报错。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetUserId` | string | 是 | 被关注用户的 userId |

```bash
curl -X POST 'http://localhost:8080/api/rally/user/follow' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{"targetUserId":"123456789"}'
```

响应：`data` 为 `null`。

```json
{ "code": 0, "message": null, "data": null }
```

---

## 2. 取消关注

- **POST** `/api/rally/user/follow/cancel`
- **幂等**：未关注时调用也返回成功。

请求体：同「关注用户」（`targetUserId`）。

```bash
curl -X POST 'http://localhost:8080/api/rally/user/follow/cancel' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{"targetUserId":"123456789"}'
```

响应：`data` 为 `null`。

---

## 3. 关注列表（我/某用户关注了谁）

- **GET** `/api/rally/user/follow/following`

Query 参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `userId` | string | 否 | 当前登录用户 | 要查询的用户；不传则查自己 |
| `lastId` | string | 否 | - | 翻页游标，传上一页最后一条的 `cursor` 值；首屏不传 |
| `size` | int | 否 | 20 | 每页数量，最小 1 |

> **游标分页**：列表按关注时间倒序返回。首屏不传 `lastId`；当 `hasMore=true` 需加载下一页时，取本页 `list` **最后一条的 `cursor` 字段**作为下一次请求的 `lastId`。游标值对前端不透明，原样回传即可。

```bash
curl 'http://localhost:8080/api/rally/user/follow/following?size=20' \
  -H 'Authorization: {token}'
```

响应 `data` 为 `PageDTO<FollowUserDTO>`：

```json
{
  "code": 0,
  "message": null,
  "data": {
    "list": [
      {
        "userId": "123456789",
        "nickname": "张三",
        "avatarUrl": "https://cdn.example.com/avatar.png?sign=...",
        "ntrpScore": 4.5,
        "isFollowed": true,
        "cursor": "1799999999999999999"
      }
    ],
    "total": null,
    "hasMore": false
  }
}
```

---

## 4. 被关注（粉丝）列表（谁关注了我/某用户）

- **GET** `/api/rally/user/follow/followers`
- Query 参数、响应结构与「关注列表」完全一致。

```bash
curl 'http://localhost:8080/api/rally/user/follow/followers?size=20' \
  -H 'Authorization: {token}'
```

### FollowUserDTO 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | string | 用户 userId |
| `nickname` | string | 昵称 |
| `avatarUrl` | string | 头像（已签名直出 URL） |
| `ntrpScore` | number | NTRP 自评分值，可能为 `null`（未建档） |
| `isFollowed` | boolean | **当前登录用户**是否已关注该用户（用于列表「关注/已关注」按钮态） |
| `cursor` | string | 翻页游标；加载下一页时取本页最后一条的该值作为 `lastId` |

> 粉丝列表中的 `isFollowed` 可用来区分「互相关注」：某粉丝的 `isFollowed=true` 表示我也关注了 TA。

---

## 5. 球员主页新增字段（PlayerHome）

球员主页接口 **GET** `/api/rally/user/profile/{userId}` 的响应 `PlayerHomeDTO` 新增 `stats` 字段：

```json
{
  "code": 0,
  "message": null,
  "data": {
    "user": { "...": "原有用户信息" },
    "stats": {
      "followerCount": 128,
      "followingCount": 56,
      "isFollowed": true
    },
    "meetup": { "...": "原有约球信息" },
    "review": { "...": "原有评价信息" },
    "level": { "...": "原有等级信息" },
    "score": { "...": "原有评分信息" },
    "video": { "...": "原有视频信息" }
  }
}
```

### PlayerHomeStatsDTO 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `followerCount` | long | 该球员的被关注数（粉丝数） |
| `followingCount` | long | 该球员的关注数 |
| `isFollowed` | boolean | **当前登录用户**是否已关注该球员（驱动主页「关注 / 已关注」按钮态） |

> 「我的档案」**GET** `/api/rally/user/profile/me` 的 `stats`（`MyProfileStatsDTO`）此前 `followerCount`/`followingCount` 恒为 0，现已接入真实关注数；该对象不含 `isFollowed`（自己无需关注自己）。

---

## 前端使用流程

### 进入球员主页
1. 调 `GET /user/profile/{userId}` 拿到 `stats`。
2. 用 `stats.isFollowed` 渲染按钮：`true` → 显示「已关注」；`false` → 显示「关注」。
3. 用 `stats.followerCount` / `stats.followingCount` 渲染粉丝数 / 关注数，并分别跳转到粉丝列表 / 关注列表（带 `userId` 参数）。

### 点击关注 / 取消关注按钮
1. 当前未关注 → 调 `POST /user/follow`；当前已关注 → 调 `POST /user/follow/cancel`。
2. 入参 `targetUserId` = 当前主页用户的 `userId`。
3. 接口成功（`code=0`）后本地翻转按钮态，并将 `followerCount` ±1（无需重新拉主页）。
4. 接口幂等，无需担心重复点击导致脏数据；失败按 `message` 提示。

### 关注 / 粉丝列表分页
1. 首屏：`GET /user/follow/following?userId=&size=20`（不传 `lastId`）。
2. 列表项用 `isFollowed` 渲染每行的关注按钮态。
3. `hasMore=true` 时，上滑加载下一页，传本页最后一条的 `cursor` 作为 `lastId`。
4. 在列表内关注/取消，复用第 1、2 个接口，成功后翻转该行 `isFollowed`。

### 自己的关注/粉丝数
- 「我的」页：`GET /user/profile/me` 的 `stats.followerCount` / `stats.followingCount`，点击进入对应列表（不传 `userId`，默认查自己）。
