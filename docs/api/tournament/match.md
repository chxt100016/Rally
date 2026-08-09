# 比赛流程（用户端）接口

**Base URL**: `/api/rally/tournament/match`（微信小程序渠道：`/api/rally/wechat/tournament/match`，入参/返参一致）

## 状态流转概览

```
MATCHED --选定订场人--> BOOKING --提交场地--> SCHEDULED
  --全员确认赛约--> PENDING_PLAY --提交结果--> PENDING_CONFIRM --全员确认结果--> COMPLETED
```

- `SCHEDULED` 阶段任一人拒绝赛约（`confirm=false` + `rejectReason`）→ 终止比赛，进入 `REJECTED`
- `SCHEDULED` 阶段任一人打回重订（`confirm=false` + `rebookReason`）→ 退回 `BOOKING`，订场人需重新提交场地
- `PENDING_CONFIRM` 阶段任一人拒绝结果（`confirm=false` + `rejectReason`）→ 退回 `PENDING_PLAY`，重新提交结果
- 所有涉及乐观锁的写接口，若返回 `code=46010`（比赛状态已变更），前端应刷新拉取最新比赛状态后重试，无需自行做重试退避。

---

## 1. 认领订场人

**POST** `/court-booker`

`MATCHED` 状态下，具备 `CAN_BOOK` 能力的参与者可认领为订场人，成功后比赛进入 `BOOKING` 状态。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛bizId |

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/court-booker' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001"}'
```

---

## 2. 提交场地信息

**POST** `/book`

仅订场人可提交，且比赛须处于 `BOOKING` 状态。场地选择模式与约球域 `CourtSelectModeEnum` 完全一致：

- `TEXT`/`MAP` 模式：传 `courtId`（球场库ID），场地名称/地址/经纬度/城市以球场库数据为准，前端传的 `courtName`/`courtLng`/`courtLat` 等会被后端用库数据覆盖。
- `FREE` 模式：不传 `courtId`，`courtName`/`courtLng`/`courtLat`/`cityCode` 均以前端传入为准。

提交成功后比赛进入 `SCHEDULED` 状态，所有参与者的赛约确认状态重置为 `PENDING`。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛bizId |
| `courtName` | `string` | 否 | 场地名称，FREE模式手填，TEXT/MAP模式由球场库回填 |
| `courtAddress` | `string` | 是 | 场地详细地址 |
| `courtSelectMode` | `string` | 否 | 球场选择模式：`TEXT`/`MAP`/`FREE` |
| `courtId` | `string` | 否 | 球场库ID，TEXT/MAP模式下必传 |
| `courtLng` | `number` | 否 | 经度，FREE模式下由前端定位传入 |
| `courtLat` | `number` | 否 | 纬度，FREE模式下由前端定位传入 |
| `cityCode` | `string` | 否 | 城市编码，FREE模式下由前端传入，TEXT/MAP模式以球场库数据为准 |
| `scheduledStartTime` | `string` | 是 | 约定的线下比赛开始时间 |
| `scheduledDuration` | `number` | 是 | 约定时长，单位：小时，最小为1 |

**响应数据**：无

**curl 示例（FREE 模式）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/book' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "matchId": "M202608100001",
    "courtName": "阳光网球场",
    "courtAddress": "上海市浦东新区世纪大道100号",
    "courtSelectMode": "FREE",
    "courtLng": 121.549,
    "courtLat": 31.238,
    "cityCode": "310100",
    "scheduledStartTime": "2026-08-15T19:00:00",
    "scheduledDuration": 2
  }'
```

**curl 示例（TEXT/MAP 模式，选中球场库数据）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/book' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "matchId": "M202608100001",
    "courtSelectMode": "MAP",
    "courtId": "C10001",
    "courtAddress": "占位，将被球场库数据覆盖",
    "scheduledStartTime": "2026-08-15T19:00:00",
    "scheduledDuration": 2
  }'
```

---

## 3. 无订场人时拒绝比赛

对应 `actionState=AWAIT_COURT_BOOKER_SELECT`。

**POST** `/reject-on-await-court-booker-select`

比赛处于 `MATCHED` 状态、尚未选出订场人时，当前比赛的参赛者可在该比赛创建匹配达到配置等待时长后拒绝比赛。等待时长读取 `tournament.match.reject_timeout_hours`，默认 48 小时。

```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/reject-on-await-court-booker-select' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "rejectReason": "DONT_WANT_PLAY"}'
```

未达到配置等待时长返回 `46028`；状态不为 `MATCHED` 返回 `46029`。

---

## 4. 订场人拒绝比赛

对应 `actionState=AWAIT_BOOKING` 或 `AWAIT_BOOKING_REBOOK`。

**POST** `/reject-on-await-booking`

仅当前订场人可以操作。比赛必须处于 `BOOKING` 状态，且当前一轮订场阶段已达到配置等待时长；首次订场从选择订场人的时间开始计算，被打回重订后从最近一次打回时间重新计算。等待时长读取 `tournament.match.reject_timeout_hours`，默认 48 小时。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛 bizId |
| `rejectReason` | `string` | 是 | 拒绝理由：`TIME_PLACE_CONFLICT`/`DONT_WANT_PLAY` |

```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/reject-on-await-booking' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "rejectReason": "TIME_PLACE_CONFLICT"}'
```

未达到配置等待时长返回 `46028`；状态不为 `BOOKING` 返回 `46027`。

---

## 5. 等待对方订场时拒绝比赛

对应 `actionState=AWAIT_BOOKING_OPPONENT`。

**POST** `/reject-on-await-booking-opponent`

仅当前比赛中非订场人的参与者可以操作。比赛必须处于 `BOOKING` 状态，且当前一轮订场阶段已达到配置等待时长。等待时长读取 `tournament.match.reject_timeout_hours`，默认 48 小时。

```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/reject-on-await-booking-opponent' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "rejectReason": "TIME_PLACE_CONFLICT"}'
```

未达到配置等待时长返回 `46028`；状态或用户角色不符合要求返回 `46030`。

---

## 6. 等待对方确认赛约时拒绝比赛

对应 `actionState=AWAIT_OPPONENT_SCHEDULE_CONFIRM`。

**POST** `/reject-on-await-opponent-schedule-confirm`

仅已提交并确认赛约、正在等待其他参与者确认的订场人可以操作。比赛必须处于 `SCHEDULED` 状态，且从本轮赛约提交起已达到配置等待时长。

```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/reject-on-await-opponent-schedule-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "rejectReason": "DONT_WANT_PLAY"}'
```

未达到配置等待时长返回 `46028`；状态或用户角色不符合要求返回 `46031`。

上述四个拒绝接口成功后，当前比赛统一进入 `REJECTED`，所有参与者回到 `WAITING` 匹配池；这些操作不校验拒绝次数上限，也不累计拒绝次数。

---

## 7. 确认/拒绝/打回赛约

**POST** `/schedule-confirm`

比赛须处于 `SCHEDULED` 状态。三种操作互斥：

- **确认**：`confirm=true`
- **拒绝比赛**（终止）：`confirm=false` + `rejectReason`（见下方枚举）
- **打回重订**（退回订场人重新提交）：`confirm=false` + `rebookReason`（见下方枚举）

全员确认后比赛进入 `PENDING_PLAY` 状态。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛bizId |
| `confirm` | `boolean` | 是 | 是否确认 |
| `rejectReason` | `string` | 否 | 拒绝比赛理由：`TIME_PLACE_CONFLICT`/`DONT_WANT_PLAY` |
| `rebookReason` | `string` | 否 | 打回重订理由：`TIME_NOT_SUITABLE`/`PLACE_NOT_SUITABLE`/`DURATION_NOT_SUITABLE` |

**响应数据**：无

**curl 示例（确认）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/schedule-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "confirm": true}'
```

**curl 示例（拒绝比赛）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/schedule-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "confirm": false, "rejectReason": "DONT_WANT_PLAY"}'
```

**curl 示例（打回重订）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/schedule-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "confirm": false, "rebookReason": "TIME_NOT_SUITABLE"}'
```

---

## 8. 提交比赛结果

**POST** `/submit-result`

比赛须处于 `PENDING_PLAY` 状态，任一参与者均可提交。提交成功后比赛进入 `PENDING_CONFIRM` 状态，所有参与者的结果确认状态重置为 `PENDING`。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛bizId |
| `winnerEntryNo` | `integer` | 是 | 获胜方报名编号（单打、双打均传一个；双打队友共用同一编号） |

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/submit-result' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "winnerEntryNo": 1}'
```

---

## 9. 确认/拒绝比赛结果

**POST** `/result-confirm`

比赛须处于 `PENDING_CONFIRM` 状态。

- **确认**：`confirm=true`。全员确认后比赛进入 `COMPLETED` 状态。
- **拒绝**（申诉）：`confirm=false` + `rejectReason`（见下方枚举）。拒绝后比赛退回 `PENDING_PLAY`，需重新提交结果。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchId` | `string` | 是 | 比赛bizId |
| `confirm` | `boolean` | 是 | 是否确认 |
| `rejectReason` | `string` | 否 | 拒绝结果理由：`DISPUTE_APPEAL`/`OPPONENT_LEVEL_MISMATCH`/`RESULT_INCORRECT` |

**响应数据**：无

**curl 示例（确认）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/result-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "confirm": true}'
```

**curl 示例（拒绝/申诉）**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/match/result-confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"matchId": "M202608100001", "confirm": false, "rejectReason": "DISPUTE_APPEAL"}'
```
