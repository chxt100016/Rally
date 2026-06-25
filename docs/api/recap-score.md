# 赛后比分接口（前端开发文档）

> 赛后收集 - 比分的「新增 / 修改 / 删除 / 查询列表」。一次操作只针对**一盘**。
> 权限：调用前需登录，且当前用户必须是该约球的可评价人（`assertReviewAvailable`），否则报权限错误。

## 通用约定

- 全局前缀：`/api/rally`，模块前缀：`/wechat/recap/score`
- 请求方式：全部 `POST`，`Content-Type: application/json`
- 用户身份：从登录态（token）自动获取，**无需在 body 里传当前用户 id**
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
| `setFormatType` 赛制 | `GAME` / `TIEBREAK` | 常规局 / 抢七 |
| `matchType` 比赛类型 | `SINGLE` / `DOUBLE` / `RALLY` | 单打 / 双打 / 拉球 |

> 枚举值传**大写英文**字符串。

### 比分字段说明（add / update 公用）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `meetupId` | string | 是 | 约球 bizId |
| `setNum` | int | 是 | 盘号，从 1 开始 |
| `setFormatType` | enum | 是 | 赛制 `GAME` / `TIEBREAK` |
| `matchType` | enum | 是 | 比赛类型 `SINGLE` / `DOUBLE` / `RALLY` |
| `sideAPlayer1` | string | 是 | A 侧选手1 userId |
| `sideAPlayer2` | string | 否 | A 侧选手2 userId（单打为 `null`） |
| `sideBPlayer1` | string | 是 | B 侧选手1 userId |
| `sideBPlayer2` | string | 否 | B 侧选手2 userId（单打为 `null`） |
| `sideAScore` | int | 是 | A 侧本盘比分 |
| `sideBScore` | int | 是 | B 侧本盘比分 |
| `sideATiebreakScore` | int | 否 | A 侧抢七比分（本盘 6:6 时填写） |
| `sideBTiebreakScore` | int | 否 | B 侧抢七比分（本盘 6:6 时填写） |
| `winSide` | string | 是 | 获胜边：`A` / `B` |

---

## 1. 新增比分

一次新增一盘。

- **URL**：`POST /api/rally/wechat/recap/score/add`
- **入参**：见上方「比分字段说明」

请求示例（双打一盘，6:4）：

```json
{
  "meetupId": "1789000000000000001",
  "setNum": 1,
  "setFormatType": "GAME",
  "matchType": "DOUBLE",
  "sideAPlayer1": "u_1001",
  "sideAPlayer2": "u_1002",
  "sideBPlayer1": "u_2001",
  "sideBPlayer2": "u_2002",
  "sideAScore": 6,
  "sideBScore": 4,
  "winSide": "A"
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/add' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","setNum":1,"setFormatType":"GAME","matchType":"DOUBLE","sideAPlayer1":"u_1001","sideAPlayer2":"u_1002","sideBPlayer1":"u_2001","sideBPlayer2":"u_2002","sideAScore":6,"sideBScore":4,"winSide":"A"}'
```

---

## 2. 修改比分

一次修改一盘。用 `bizId` 定位记录 + `version` 乐观锁防止并发覆盖。

- **URL**：`POST /api/rally/wechat/recap/score/update`
- **入参**：在「比分字段说明」基础上**额外**增加：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bizId` | string | 是 | 目标盘记录 bizId（列表查询返回，定位唯一记录） |
| `version` | int | 是 | 乐观锁版本号（取自上次查询返回值） |

> ⚠️ `version` 必须传当前列表里拿到的最新值；若期间被他人改动，更新会失败（比分冲突），需重新拉取最新数据后再提交。

请求示例（把上面那盘改成 7:6(7:3) 抢七）：

```json
{
  "meetupId": "1789000000000000001",
  "bizId": "1789000000000000099",
  "version": 1,
  "setNum": 1,
  "setFormatType": "TIEBREAK",
  "matchType": "DOUBLE",
  "sideAPlayer1": "u_1001",
  "sideAPlayer2": "u_1002",
  "sideBPlayer1": "u_2001",
  "sideBPlayer2": "u_2002",
  "sideAScore": 7,
  "sideBScore": 6,
  "sideATiebreakScore": 7,
  "sideBTiebreakScore": 3,
  "winSide": "A"
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/update' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","bizId":"1789000000000000099","version":1,"setNum":1,"setFormatType":"TIEBREAK","matchType":"DOUBLE","sideAPlayer1":"u_1001","sideAPlayer2":"u_1002","sideBPlayer1":"u_2001","sideBPlayer2":"u_2002","sideAScore":7,"sideBScore":6,"sideATiebreakScore":7,"sideBTiebreakScore":3,"winSide":"A"}'
```

---

## 3. 删除比分

一次删除一盘。按 `bizId` 定位（bizId 为雪花，永不复用，天然防 ABA）。

- **URL**：`POST /api/rally/wechat/recap/score/delete`
- **入参**：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `meetupId` | string | 是 | 约球 bizId |
| `bizId` | string | 是 | 目标盘记录 bizId |

请求示例：

```json
{
  "meetupId": "1789000000000000001",
  "bizId": "1789000000000000099"
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/delete' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","bizId":"1789000000000000099"}'
```

---

## 4. 查询我的比分列表（无限下拉）

查询当前登录用户的比分记录，支持按胜负和单双打筛选，游标分页。

- **URL**：`POST /api/rally/wechat/recap/score/list`
- **权限**：登录即可，无需约球参与资格

### 入参

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `result` | enum | 否 | 胜负筛选：`WIN` / `LOSE`，**不传 = 全部** |
| `matchType` | enum | 否 | 赛制筛选：`SINGLE` / `DOUBLE`，**不传 = 全部** |
| `lastId` | string | 否 | 游标，上一页返回的 `nextCursor`；**不传 = 第一页** |
| `pageSize` | int | 否 | 每页条数，默认 20 |

请求示例（第一页，查全部）：

```json
{}
```

请求示例（第一页，筛选单打胜场）：

```json
{
  "result": "WIN",
  "matchType": "SINGLE"
}
```

请求示例（加载下一页）：

```json
{
  "lastId": "1789000000000000099"
}
```

curl：

```bash
# 第一页
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list' \
  -H 'Content-Type: application/json' \
  -d '{}'

# 下一页（传上一页的 nextCursor）
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/list' \
  -H 'Content-Type: application/json' \
  -d '{"lastId":"1789000000000000099"}'
```

### 返回示例

```json
{
  "code": 0,
  "data": {
    "total": 35,
    "singleCount": 20,
    "doubleCount": 15,
    "list": [
      {
        "bizId": "1789000000000000099",
        "resultType": "WIN",
        "resultTypeShow": "胜",
        "matchType": "SINGLE",
        "matchTypeShow": "单打",
        "setFormat": "GAME",
        "setFormatShow": "正赛",
        "date": "06-20",
        "sideAPlayer1AvatarUrl": "https://cdn.example.com/avatar1.jpg",
        "sideAPlayer2AvatarUrl": null,
        "sideAScore": "6",
        "sideBPlayer1AvatarUrl": "https://cdn.example.com/avatar2.jpg",
        "sideBPlayer2AvatarUrl": null,
        "sideBScore": "3"
      }
    ],
    "hasMore": true,
    "nextCursor": "1789000000000000099"
  }
}
```

### 返回字段说明

| 字段 | 说明 |
| --- | --- |
| `total` | 该用户全部比分总数（**不受筛选条件影响**，用于 Tab 角标） |
| `singleCount` | 全部单打场数（不受筛选影响） |
| `doubleCount` | 全部双打场数（不受筛选影响） |
| `list` | 当前页筛选后的比分列表，按比赛日期倒序 |
| `list[].bizId` | 比分记录 ID，用作游标回传 |
| `list[].resultType` | 枚举：`WIN` / `LOSE` |
| `list[].resultTypeShow` | 展示文案：`胜` / `负` |
| `list[].matchType` | 枚举：`SINGLE` / `DOUBLE` |
| `list[].matchTypeShow` | 展示文案：`单打` / `双打` |
| `list[].setFormat` | 枚举：`GAME` / `TIEBREAK` |
| `list[].setFormatShow` | 展示文案 |
| `list[].date` | 比赛日期，格式 `MM-dd` |
| `list[].sideAPlayer1AvatarUrl` | A侧选手1头像（已签名） |
| `list[].sideAPlayer2AvatarUrl` | A侧选手2头像（已签名），单打为 `null` |
| `list[].sideAScore` | A侧本盘比分（字符串） |
| `list[].sideBPlayer1AvatarUrl` | B侧选手1头像（已签名） |
| `list[].sideBPlayer2AvatarUrl` | B侧选手2头像（已签名），单打为 `null` |
| `list[].sideBScore` | B侧本盘比分（字符串） |
| `hasMore` | 是否还有更多数据 |
| `nextCursor` | 下一页游标（`hasMore` 为 `true` 时有值），原样回传给 `lastId` |

---

## 前端联调要点

1. **新增**不需要 `bizId` / `version`；**修改**两者都必传；**删除**只需 `meetupId` + `bizId`。
2. 单打时把 `sideAPlayer2` / `sideBPlayer2` 设为 `null` 或不传。
3. 本盘打到 6:6 进入抢七时，填 `sideATiebreakScore` / `sideBTiebreakScore`，并将 `setFormatType` 置为 `TIEBREAK`。
4. 修改流程：先查询拿到每盘的 `bizId` + `version` → 提交修改 → 若返回比分冲突错误，重新查询刷新 `version` 后再试。
5. **列表接口**的 `total/singleCount/doubleCount` 始终是全量统计，可用于渲染 Tab 角标；`list` 才是筛选后的当前页内容。
6. 无限下拉：`hasMore` 为 `true` 时，将 `nextCursor` 原样传给下一次请求的 `lastId`；`hasMore` 为 `false` 时停止加载。切换筛选条件时，清空 `lastId` 重新从第一页开始。
6. 所有接口成功判断以 `code === 0` 为准。
