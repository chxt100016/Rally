# 比分记录 API 文档

基础路径：`/api/rally/wechat/recap/score`

---

## 1. 获取我的比分统计

查询当前登录用户的比赛统计数据，支持按比赛类型（单打/双打）过滤。

### 请求

```
GET /stats/me
```

### 查询参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| matchType | string | 否 | 比赛类型过滤，可选值：`SINGLE`（单打）、`DOUBLE`（双打）。不传则返回全部数据 |

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 42,
    "wins": 28,
    "losses": 14,
    "winRate": "66.7",
    "streakType": "WIN",
    "streakCount": 3
  }
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| total | long | 总场次（根据 matchType 过滤后） |
| wins | long | 胜场次（根据 matchType 过滤后） |
| losses | long | 负场次（根据 matchType 过滤后） |
| winRate | string | 胜率，百分比保留一位小数，无数据时返回 `"--"` |
| streakType | string | 当前连胜/连败状态，`"WIN"` 表示连胜、`"LOSE"` 表示连败，无数据时返回 `null` |
| streakCount | long | 当前连胜/连败场数，无数据时返回 `null` |

> **说明**：所有字段均根据 `matchType` 过滤后计算，传 `SINGLE` 只统计单打，传 `DOUBLE` 只统计双打，不传则统计全部。

### curl 示例

```bash
# 查询全部统计
curl -X GET 'http://localhost:8080/api/rally/wechat/recap/score/stats/me'

# 只看单打统计
curl -X GET 'http://localhost:8080/api/rally/wechat/recap/score/stats/me?matchType=SINGLE'

# 只看双打统计
curl -X GET 'http://localhost:8080/api/rally/wechat/recap/score/stats/me?matchType=DOUBLE'
```

---

## 2. 查询我的比分记录列表

分页查询当前登录用户的比赛记录，支持按比赛类型和约球活动过滤，基于游标分页。

### 请求

```
POST /list/me
Content-Type: application/json
```

### 请求体

```json
{
  "matchType": "SINGLE",
  "meetupId": "1234567890",
  "lastId": "1800000000000001",
  "pageSize": 20
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| matchType | string | 否 | 比赛类型过滤，可选值：`SINGLE`（单打）、`DOUBLE`（双打）。不传则不过滤 |
| meetupId | string | 否 | 约球活动 ID 过滤，不传则不过滤 |
| lastId | string | 否 | 上一页最后一条记录的 bizId，用于游标分页。首次请求不传 |
| pageSize | integer | 否 | 每页条数，默认 20 |

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "bizId": "1800000000000001",
        "meetupId": "1234567890",
        "resultType": "WIN",
        "resultTypeShow": "胜",
        "matchType": "DOUBLE",
        "matchTypeShow": "双打",
        "setFormat": "GAME",
        "setFormatShow": "局",
        "date": "06-25",
        "myScore": "6",
        "opponentScore": "3",
        "teammateId": "user_003",
        "teammateNickname": "李四",
        "teammateAvatarUrl": "https://cdn.example.com/avatar/003.jpg",
        "opponent1Id": "user_002",
        "opponent1Nickname": "张三",
        "opponent1AvatarUrl": "https://cdn.example.com/avatar/002.jpg",
        "opponent2Id": "user_004",
        "opponent2Nickname": "王五",
        "opponent2AvatarUrl": "https://cdn.example.com/avatar/004.jpg"
      }
    ],
    "hasMore": true,
    "nextCursor": "1800000000000001"
  }
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 比分记录列表 |
| list[].bizId | string | 比分记录唯一 ID（雪花 ID） |
| list[].meetupId | string | 关联约球活动 ID |
| list[].resultType | string | 比赛结果枚举，`WIN`/`LOSE` |
| list[].resultTypeShow | string | 比赛结果展示，`"胜"`/`"负"` |
| list[].matchType | string | 比赛类型枚举，`SINGLE`/`DOUBLE`/`RALLY` |
| list[].matchTypeShow | string | 比赛类型展示，`"单打"`/`"双打"`/`"拉球"` |
| list[].setFormat | string | 赛制枚举，`GAME`（局）/`TIEBREAK`（抢分） |
| list[].setFormatShow | string | 赛制展示，`"局"`/`"抢分"` |
| list[].date | string | 比赛日期，格式 `MM-dd` |
| list[].myScore | string | 我的得分 |
| list[].opponentScore | string | 对手得分 |
| list[].teammateId | string | 队友用户 ID（单打时为 null） |
| list[].teammateNickname | string | 队友昵称（单打时为 null） |
| list[].teammateAvatarUrl | string | 队友头像 URL（单打时为 null） |
| list[].opponent1Id | string | 对手1用户 ID |
| list[].opponent1Nickname | string | 对手1昵称 |
| list[].opponent1AvatarUrl | string | 对手1头像 URL（七牛签名 URL） |
| list[].opponent2Id | string | 对手2用户 ID（单打时为 null） |
| list[].opponent2Nickname | string | 对手2昵称（单打时为 null） |
| list[].opponent2AvatarUrl | string | 对手2头像 URL（单打时为 null） |
| hasMore | boolean | 是否还有更多数据 |
| nextCursor | string | 下一页游标（即本页最后一条的 bizId），无更多数据时为 `null` |

### curl 示例

```bash
# 首次查询（默认分页）
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list/me' \
  -H 'Content-Type: application/json' \
  -d '{}'

# 按单打过滤
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list/me' \
  -H 'Content-Type: application/json' \
  -d '{"matchType": "SINGLE"}'

# 按约球活动过滤
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list/me' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId": "1234567890"}'

# 翻页（传入上一页返回的 nextCursor）
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list/me' \
  -H 'Content-Type: application/json' \
  -d '{"lastId": "1800000000000001", "pageSize": 20}'
```

---

## 3. 删除比分记录

删除当前用户指定约球活动中的一条比分记录（按 bizId 定位）。

### 请求

```
POST /delete
Content-Type: application/json
```

### 请求体

```json
{
  "meetupId": "1234567890",
  "bizId": "1800000000000001"
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| meetupId | string | 是 | 约球活动 ID |
| bizId | string | 是 | 要删除的比分记录 ID（雪花 ID） |

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### curl 示例

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/delete' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId": "1234567890", "bizId": "1800000000000001"}'
```
