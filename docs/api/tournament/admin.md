# 赛事管理（运营后台）接口

**Base URL**: `/api/rally/tournament/admin`

---

## 1. 创建赛事草稿

**POST** `/create`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentName` | `string` | 是 | 赛事名称，不超过128字符 |
| `posterKey` | `string` | 否 | 活动海报图片key（对象存储） |
| `cityCode` | `string` | 是 | 城市编码 |
| `ntrpLevel` | `string` | 是 | NTRP等级：3.0/3.5/4.0... |
| `genderLimit` | `string` | 是 | 性别限制：`ALL`/`MALE`/`FEMALE` |
| `totalSlots` | `number` | 是 | 正赛签位：16/32/64 |
| `offlineFromRound` | `number` | 是 | 几强后转线下：4/8/16 |
| `qualifierGroupSize` | `number` | 是 | 资格赛每组人数，最小2 |
| `entryFee` | `number` | 是 | 报名费，单位：分，不能为负 |
| `registrationStartTime` | `string` | 是 | 报名开始时间，格式 `yyyy-MM-dd'T'HH:mm:ss` |
| `registrationEndTime` | `string` | 否 | 报名截止时间，可空 |
| `qualifierStartTime` | `string` | 是 | 资格赛开始时间 |
| `qualifierEndTime` | `string` | 否 | 资格赛截止时间，可空表示永久有效 |
| `qualifierRejectLimit` | `number` | 是 | 资格赛阶段拒绝次数上限，不能为负 |
| `mainDrawRejectLimit` | `number` | 是 | 正赛阶段拒绝次数上限，不能为负 |
| `matchRuleDescription` | `string` | 否 | 比赛规则描述，纯文本，支持 `\n` 换行，不超过5000字符 |

**响应数据** `data`

| 字段 | 类型 | 说明 |
|---|---|---|
| `tournamentId` | `string` | 新建赛事的 bizId |

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/create' \
  -H 'Content-Type: application/json' \
  -d '{
    "tournamentName": "2026春季网球公开赛",
    "cityCode": "310100",
    "ntrpLevel": "3.5",
    "genderLimit": "ALL",
    "totalSlots": 32,
    "offlineFromRound": 8,
    "qualifierGroupSize": 2,
    "entryFee": 5000,
    "registrationStartTime": "2026-08-01T00:00:00",
    "qualifierStartTime": "2026-08-10T00:00:00",
    "qualifierRejectLimit": 1,
    "mainDrawRejectLimit": 1,
    "matchRuleDescription": "比赛规则：\n1. 每场比赛采用三盘两胜制\n2. 每盘6局，抢七制\n3. 迟到15分钟视为弃权"
  }'
```

---

## 2. 编辑草稿

**POST** `/update`

只能编辑 `DRAFT` 状态的赛事。请求参数为「创建赛事草稿」的全部字段 + `tournamentId`。

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |
| 其余字段 | - | - | 同「创建赛事草稿」，全部字段生效替换 |

**响应数据**：无（`data` 为 `null`）

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/update' \
  -H 'Content-Type: application/json' \
  -d '{
    "tournamentId": "T202608010001",
    "tournamentName": "2026春季网球公开赛（修订）",
    "cityCode": "310100",
    "ntrpLevel": "3.5",
    "genderLimit": "ALL",
    "totalSlots": 32,
    "offlineFromRound": 8,
    "qualifierGroupSize": 2,
    "entryFee": 5000,
    "registrationStartTime": "2026-08-01T00:00:00",
    "qualifierStartTime": "2026-08-10T00:00:00",
    "qualifierRejectLimit": 1,
    "mainDrawRejectLimit": 1,
    "matchRuleDescription": "更新后的比赛规则：\n1. 采用长盘制\n2. 决胜盘抢十"
  }'
```

---

## 3. 激活赛事

**POST** `/activate`

将 `DRAFT` 状态的赛事转为 `ACTIVE`，激活后开放报名/开始比赛匹配流程。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/activate' \
  -H 'Content-Type: application/json' \
  -d '{"tournamentId": "T202608010001"}'
```

---

## 4. 废弃赛事

**POST** `/abandon`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentId` | `string` | 是 | 赛事bizId |
| `reason` | `string` | 否 | 废弃原因 |

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/abandon' \
  -H 'Content-Type: application/json' \
  -d '{"tournamentId": "T202608010001", "reason": "报名人数不足"}'
```

---

## 5. 后台赛事列表

**POST** `/list`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `cityCode` | `string` | 否 | 城市编码筛选 |
| `status` | `string` | 否 | 状态筛选：`DRAFT`/`ACTIVE`/`ABANDONED` |
| `ntrpLevel` | `string` | 否 | NTRP等级筛选 |
| `pageNum` | `number` | 是 | 页码，从1开始 |
| `pageSize` | `number` | 是 | 每页条数，1~100 |

**响应数据** `data`（`PageDTO<TournamentAdminItemDTO>`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `total` | `number` | 总条数 |
| `pageNum` | `number` | 当前页码 |
| `pageSize` | `number` | 每页条数 |
| `list` | `TournamentAdminItemDTO[]` | 赛事列表项 |

`TournamentAdminItemDTO` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `tournamentId` | `string` | 赛事bizId |
| `tournamentName` | `string` | 赛事名称 |
| `posterUrl` | `string\|null` | 海报图片访问地址（签名URL） |
| `cityCode` | `string` | 城市编码 |
| `cityName` | `string` | 城市名称 |
| `ntrpLevel` | `string` | NTRP等级 |
| `genderLimit` | `string` | 性别限制 |
| `totalSlots` | `number` | 正赛签位 |
| `offlineFromRound` | `number` | 转线下轮次 |
| `qualifierGroupSize` | `number` | 资格赛每组人数 |
| `entryFee` | `number` | 报名费（分） |
| `registrationStartTime` | `string` | 报名开始时间 |
| `registrationEndTime` | `string\|null` | 报名截止时间 |
| `qualifierStartTime` | `string` | 资格赛开始时间 |
| `qualifierEndTime` | `string\|null` | 资格赛截止时间 |
| `qualifierRejectLimit` | `number` | 资格赛拒绝次数上限 |
| `mainDrawRejectLimit` | `number` | 正赛拒绝次数上限 |
| `status` | `string` | 赛事状态 |
| `currentFilledSlots` | `number` | 当前已支付锁定的正赛席位数 |
| `createTime` | `string` | 创建时间 |

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/list' \
  -H 'Content-Type: application/json' \
  -d '{"status": "ACTIVE", "pageNum": 1, "pageSize": 20}'
```
