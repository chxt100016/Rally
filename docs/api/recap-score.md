# 赛后比分接口（前端开发文档）

> 赛后收集 - 比分的「新增 / 修改 / 删除」。一次操作只针对**一盘**。
> 权限：调用前后端需登录，且当前用户必须是该约球的可评价人（`assertReviewAvailable`），否则报权限错误。

## 通用约定

- 全局前缀：`/api/rally`，模块前缀：`/wechat/recap`
- 请求方式：全部 `POST`，`Content-Type: application/json`
- 用户身份：从登录态（token）自动获取，**无需在 body 里传当前用户 id**
- 统一返回结构：

```json
{
  "code": 0,        // 0 = 成功；非 0 = 业务错误
  "message": null,  // 失败时为错误描述
  "data": null      // 比分接口成功时无数据返回
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
  "sideBScore": 4
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/add' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","setNum":1,"setFormatType":"GAME","matchType":"DOUBLE","sideAPlayer1":"u_1001","sideAPlayer2":"u_1002","sideBPlayer1":"u_2001","sideBPlayer2":"u_2002","sideAScore":6,"sideBScore":4}'
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
  "sideBTiebreakScore": 3
}
```

curl：

```bash
curl -X POST 'http://localhost:8080/api/rally/wechat/recap/score/update' \
  -H 'Content-Type: application/json' \
  -d '{"meetupId":"1789000000000000001","bizId":"1789000000000000099","version":1,"setNum":1,"setFormatType":"TIEBREAK","matchType":"DOUBLE","sideAPlayer1":"u_1001","sideAPlayer2":"u_1002","sideBPlayer1":"u_2001","sideBPlayer2":"u_2002","sideAScore":7,"sideBScore":6,"sideATiebreakScore":7,"sideBTiebreakScore":3}'
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

## 前端联调要点

1. **新增**不需要 `bizId` / `version`；**修改**两者都必传；**删除**只需 `meetupId` + `bizId`。
2. 单打时把 `sideAPlayer2` / `sideBPlayer2` 设为 `null` 或不传。
3. 本盘打到 6:6 进入抢七时，填 `sideATiebreakScore` / `sideBTiebreakScore`，并将 `setFormatType` 置为 `TIEBREAK`。
4. 修改流程：先查询拿到每盘的 `bizId` + `version` → 提交修改 → 若返回比分冲突错误，重新查询刷新 `version` 后再试。
5. 所有接口成功判断以 `code === 0` 为准。
</content>
</invoke>
