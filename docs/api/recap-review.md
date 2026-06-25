# 赛后评价接口（前端开发文档）

> 赛后收集 - 评价的「跳过 / 提交 / 查询我的评价摘要」。
> 权限：调用前需登录；写操作还要求当前用户是该约球的可评价人（`assertReviewAvailable`），否则报权限/状态错误。

## 通用约定

- 全局前缀：`/api/rally`，模块前缀：`/wechat/recap/review`
- 请求方式：写操作 `POST`，查询 `GET`，`Content-Type: application/json`
- 用户身份（评价发起人）：从登录态（token）自动获取，**无需在 body 里传当前用户 id**
- 统一返回结构：

```json
{
  "code": 0,        // 0 = 成功；非 0 = 业务错误
  "message": null,  // 失败时为错误描述
  "data": null      // 查询接口有数据，写操作为 null
}
```

### 枚举说明

| 枚举 | 取值 | 含义 |
| --- | --- | --- |
| `reviews[].type` 评价维度 | `LEVEL_VOTE` / `TAG` / `ATTENDANCE_VOTE` | NTRP 三元投票 / 个性化标签 / 出勤标签 |

> 枚举值传**大写英文**字符串。

### 评价维度与 value 取值

| `type` | `value` 取值 | 说明 |
| --- | --- | --- |
| `LEVEL_VOTE` | `HIGHER` / `SAME` / `LOWER` | 对方水平：高了 / 差不多（默认） / 低了 |
| `ATTENDANCE_VOTE` | `ON_TIME` / `LATE` / `NO_SHOW` | 出勤情况：准时（默认） / 迟到 / 爽约 |
| `TAG` | 自定义标签，多个标签用英文逗号 `,` 分隔，如 `"友善,技术好"` | 个性化标签（可多选），非空即可 |

> `LEVEL_VOTE` / `ATTENDANCE_VOTE` 若传入枚举值之外的字符串，会报 `RECAP_REVIEW_INVALID_VALUE`。

---

## 1. 跳过评价

一键标记「无需评价」，当前用户的报名状态变更为 `SKIPPED`，从待评价列表移除。

- **URL**：`POST /api/rally/wechat/recap/review/skip`

### 入参

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `meetupId` | string | 是 | 约球 bizId |

请求示例：

```json
{
  "meetupId": "1789000000000000001"
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/review/skip' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001"}'
```

---

## 2. 提交评价

一次评价一个用户，可同时提交多个维度。

- **URL**：`POST /api/rally/wechat/recap/review`

### 入参

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `meetupId` | string | 是 | 约球 bizId |
| `toUserId` | string | 是 | 被评价人 userId（一次只评价一个用户） |
| `reviews` | array | 否 | 评价项列表，每个维度一条，见下表 |
| `reviews[].type` | enum | 是 | 评价维度 `LEVEL_VOTE` / `TAG` / `ATTENDANCE_VOTE` |
| `reviews[].value` | string | 是 | 评价值，取值规则见上方「评价维度与 value 取值」 |

> 同一 `meetupId` + `toUserId` + `type` 组合重复提交时为**覆盖更新**（取最新一次的 `value`），可用于修改之前的评价。

请求示例（同时提交水平投票、出勤、标签三个维度）：

```json
{
  "meetupId": "1789000000000000001",
  "toUserId": "u_2001",
  "reviews": [
    { "type": "LEVEL_VOTE", "value": "SAME" },
    { "type": "ATTENDANCE_VOTE", "value": "ON_TIME" },
    { "type": "TAG", "value": "友善,技术好" }
  ]
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/review' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","toUserId":"u_2001","reviews":[{"type":"LEVEL_VOTE","value":"SAME"},{"type":"ATTENDANCE_VOTE","value":"ON_TIME"},{"type":"TAG","value":"友善,技术好"}]}'
```

### 错误码

| code | 说明 | 触发场景 |
| --- | --- | --- |
| `41001` | 约球不存在 | `meetupId` 不存在 |
| `41008` | 你未报名该约球 | 当前用户既非创建者也未报名该约球 |
| `42002` | 评价已超期 | 超过约球结束时间 + 评价截止天数（`REVIEW_DEADLINE_DAYS`） |
| `42005` | 约球不可评价 | 约球状态不是「进行中/已结束」 |
| `43004` | 评价值不合法 | `LEVEL_VOTE` / `ATTENDANCE_VOTE` 的 `value` 不在对应枚举范围内 |

---

## 3. 查询我收到的评价摘要

查询当前登录用户收到的所有评价汇总，含总数、各维度数量及 Top 标签。

- **URL**：`GET /api/rally/wechat/recap/review/my`
- **权限**：登录即可，无需约球参与资格
- **入参**：无

curl：

```bash
curl -X GET 'http://localhost:8080/api/rally/wechat/recap/review/my'
```

### 返回示例

```json
{
  "code": 0,
  "data": {
    "total": 58,
    "levelVoteCount": 20,
    "attendanceVoteCount": 18,
    "tagCount": 20,
    "tags": [
      { "name": "友善", "count": 12 },
      { "name": "技术好", "count": 9 },
      { "name": "守时", "count": 8 },
      { "name": "稳定", "count": 6 },
      { "name": "有趣", "count": 3 }
    ]
  }
}
```

### 返回字段说明

| 字段 | 说明 |
| --- | --- |
| `total` | 收到的评价总数（`levelVoteCount + attendanceVoteCount + tagCount`） |
| `levelVoteCount` | 收到的水平投票（`LEVEL_VOTE`）次数 |
| `attendanceVoteCount` | 收到的出勤评价（`ATTENDANCE_VOTE`）次数 |
| `tagCount` | 收到的标签（`TAG`）总数（多标签按逗号拆分后分别计数） |
| `tags` | Top 5 标签列表，按数量降序 |
| `tags[].name` | 标签名 |
| `tags[].count` | 该标签被打次数 |

---

## 前端联调要点

1. `reviews` 数组内每个 `type` 只能出现一次；如需同时评价多个维度，放在同一次请求的数组里。
2. `TAG` 维度若用户多选了多个标签，前端需自行拼接为逗号分隔字符串后传给 `value`。
3. 重复提交同一 `meetupId` + `toUserId` + `type` 会覆盖旧值，可用于「修改评价」场景，无需先查询旧记录。
4. 评价有截止时间限制（约球结束时间 + N 天），超时提交会报 `42002`，前端应在 UI 上提前给出提示/禁用入口。
5. 「查询我的评价摘要」用于我的主页展示，与约球无关，直接 GET 即可，无需传任何参数。
6. 所有接口成功判断以 `code === 0` 为准。
