# 赛事报名（用户端）接口

**Base URL**: `/api/rally/tournament/entry`（微信小程序渠道：`/api/rally/wechat/tournament/entry`，入参/返参一致）

---

## 1. 报名

**POST** `/join`

需赛事处于 `ACTIVE` 状态且在报名时间窗口内，同一用户对同一赛事只能报名一次。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |
| `partnerId` | `string` | 否 | 双打搭档用户ID |
| `preferredDistricts` | `string[]` | 否 | 活动区域 |
| `courtAbility` | `string` | 是 | 场地能力：`CAN_BOOK`/`CANNOT_BOOK` |
| `availableTimes` | `string[]` | 否 | 可比赛时间 |

**响应数据** `data`（`TournamentEntryDTO`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `entryId` | `string` | 报名记录bizId |
| `tournamentId` | `string` | 赛事bizId |
| `partnerId` | `string\|null` | 双打搭档用户ID |
| `preferredDistricts` | `string[]` | 活动区域 |
| `courtAbility` | `string` | 场地能力 |
| `availableTimes` | `string[]` | 可比赛时间 |
| `stage` | `string` | 报名阶段：`QUALIFY`/`MAIN` |
| `status` | `string` | 报名状态：`WAITING`/`IN_MATCH`/`PAYING`/`ELIMINATED`/`WITHDRAWN` |
| `currentRound` | `string` | 当前轮次 |

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/entry/join' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "tournamentId": "T202608010001",
    "courtAbility": "CAN_BOOK",
    "preferredDistricts": ["浦东", "徐汇"],
    "availableTimes": ["周末上午", "周末下午"]
  }'
```

---

## 2. 修改报名偏好

**POST** `/update`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |
| `preferredDistricts` | `string[]` | 否 | 活动区域 |
| `courtAbility` | `string` | 是 | 场地能力：`CAN_BOOK`/`CANNOT_BOOK` |
| `availableTimes` | `string[]` | 否 | 可比赛时间 |

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/entry/update' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "tournamentId": "T202608010001",
    "courtAbility": "CANNOT_BOOK",
    "availableTimes": ["工作日晚上"]
  }'
```

---

## 3. 退出赛事

**POST** `/withdraw`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |

**响应数据** `data`（`TournamentWithdrawResultDTO`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `refundTriggered` | `boolean` | 是否触发退款（正赛阶段退出时为true，当前退款功能暂未开放） |

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/entry/withdraw' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"tournamentId": "T202608010001"}'
```

---

## 4. 支付报名费

**POST** `/pay`

资格赛获胜后（entry status 变为 `PAYING`），用户支付锁定正赛席位。校验 entry 为 PAYING 状态且赛事未满员，创建支付单并返回微信小程序拉起支付参数。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |

**响应数据** `data`（`PrepayResult`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `prepayId` | `string` | 微信预支付ID |
| `timeStamp` | `string` | 时间戳 |
| `nonceStr` | `string` | 随机字符串 |
| `packageVal` | `string` | 订单详情扩展字符串（格式：`prepay_id=xxx`） |
| `signType` | `string` | 签名方式 |
| `paySign` | `string` | 签名 |

前端拿到 `PrepayResult` 后调用 `wx.requestPayment` 拉起支付。支付成功后微信异步回调推进 entry 状态（PAYING→WAITING，stage QUALIFY→MAIN，写 paidTime），并原子扣正赛席位。

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/entry/pay' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"tournamentId": "T202608010001"}'
```
