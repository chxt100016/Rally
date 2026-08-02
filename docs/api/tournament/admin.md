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
| `totalSlots` | `number` | 是 | 正赛签位：2 到 64 的 2 次方（2/4/8/16/32/64） |
| `offlineFromRound` | `string` | 是 | 转线下起始轮次，须小于 `totalSlots`；可选 `QUALIFIER`/`ROUND_64`/`ROUND_32`/`ROUND_16`/`ROUND_8`/`ROUND_4`/`FINAL` |
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
    "offlineFromRound": "ROUND_8",
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
    "offlineFromRound": "ROUND_8",
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
| `offlineFromRound` | `string` | 转线下轮次 |
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

---

## 6. 手动执行赛事批量匹配

**POST** `/match/run`

不传请求体时，扫描所有 `ACTIVE` 且已到资格赛开始时间的赛事，并只匹配各赛事的 `currentRound`。该接口与定时任务调用同一应用服务，不受 `job.tournamentMatch.enabled` 开关影响。

传入 `tournamentId` 和 `manualGroups` 时，运营可直接指定当前轮次的 `entryNo` 分组；每个内部数组代表一场比赛。正赛每组必须有 2 个 `entryNo`，资格赛每组必须等于 `qualifierGroupSize`。指定的队伍必须唯一、属于当前轮次且处于 `WAITING`，否则请求失败。指定分组会优先生成比赛，之后其余 `WAITING` 队伍继续自动匹配补齐。

**请求参数**：可选。例如手工指定两场正赛：

```json
{
  "tournamentId": "123456",
  "manualGroups": [[1, 2], [3, 4]],
  "excludedEntryNos": [5, 6]
}
```

`excludedEntryNos` 为可选字段，传入时必须同时传 `tournamentId`；这些队仅在本次匹配中被排除，报名仍保持 `WAITING`。

**响应数据**：无

**curl 示例**
```bash
curl -X POST 'http://localhost:8080/api/rally/tournament/admin/match/run'
```

### 7. 取消未提交订场信息的比赛

**POST** `/match/cancel`

按赛事批量取消所有 `MATCHED` 或 `BOOKING` 状态的比赛。取消会删除比赛及参与者记录，并把参赛者恢复为当前轮次的 `WAITING`，可随后再次自动匹配或使用手工分组。已提交订场信息的比赛不可取消。

```json
{
  "tournamentId": "123456"
}
```
