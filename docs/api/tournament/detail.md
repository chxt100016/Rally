# 赛事落地页详情（聚合查询）接口

**Base URL**: `/api/rally/tournament/detail`（微信小程序渠道：`/api/rally/wechat/tournament/detail`，入参/返参一致）

只读聚合接口，赛事落地页收口为一个接口：聚合赛事信息、公开进程、当前用户报名与比赛、显式 `actionState`、个人时间线、签表、信用记录。前端只按 `actionState` switch-case 渲染"当前待办卡片"，不自行拼状态。

---

## 1. 赛事详情

**GET** `/{bizId}`

`userId` 从登录态取，**支持匿名访问**（未登录时只返回 `tournament`/`progress`/`bracket` 公开区块，`actionState` 固定为 `NOT_REGISTERED`）。

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `bizId` | `string` | 是 | 赛事bizId |

**响应数据** `data`（`TournamentDetailDTO`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `tournament` | `TournamentDTO` | 赛事基础信息 |
| `progress` | `TournamentProgressDTO` | 公开进程，所有访问者可见 |
| `myEntry` | `TournamentEntryDTO\|null` | 当前用户报名信息，未报名/未登录为 null |
| `myCurrentMatch` | `MyCurrentMatchDTO\|null` | 当前用户进行中的比赛，无则为 null |
| `actionState` | `string` | 显式待办状态，驱动"当前待办卡片"渲染 |
| `myTimeline` | `TournamentTimelineEventDTO[]` | 个人视角事件流，不含未登录/未报名场景 |
| `bracket` | `TournamentBracketDTO` | 签表对阵图数据 |
| `rejectRecords` | `TournamentRejectRecordDTO[]` | 赛事所有参赛者的拒绝比赛次数统计（不限于本人） |

### TournamentDTO（赛事基础信息）

| 字段 | 类型 | 说明 |
|---|---|---|
| `tournamentId` | `string` | 赛事bizId |
| `tournamentName` | `string` | 赛事名称 |
| `posterUrl` | `string\|null` | 海报图片访问地址（签名URL） |
| `cityName` | `string` | 城市名称 |
| `ntrpLevel` | `string` | NTRP等级 |
| `genderLimit` | `string` | 性别限制：`ALL`/`MALE`/`FEMALE` |
| `entryFee` | `number` | 报名费，单位：分 |
| `registrationStartTime` | `string` | 报名开始时间 |
| `registrationEndTime` | `string\|null` | 报名截止时间 |
| `qualifierStartTime` | `string` | 资格赛开始时间 |
| `qualifierEndTime` | `string\|null` | 资格赛截止时间 |
| `offlineFromRound` | `number` | 几强后转线下 |
| `matchRuleDescription` | `string\|null` | 比赛规则描述，纯文本，支持 `\n` 换行 |
| `displayStatus` | `string` | 展示状态（基于时间计算）：`NOT_STARTED`/`REGISTRATION`/`IN_PROGRESS`/`ENDED`/`ABANDONED` |
| `displayStatusShow` | `string` | 展示状态中文：未开始/报名中/进行中/已结束/已废弃 |

### TournamentProgressDTO（公开进程）

| 字段 | 类型 | 说明 |
|---|---|---|
| `entryCount` | `number` | 报名人数（赛事所有报名记录总数） |
| `currentFilledSlots` | `number` | 已支付锁定的正赛席位数 |
| `totalSlots` | `number` | 正赛总签位数 |
| `currentRound` | `string\|null` | 当前公开进行的轮次，尚无比赛时为 null |
| `currentRoundTotalMatches` | `number` | 本轮总比赛场数 |
| `currentRoundCompletedMatches` | `number` | 本轮已完成场数 |
| `totalMatchCount` | `number` | 当前赛事已生成的比赛总场数（资格赛+正赛累计） |
| `registrationEndTime` | `string\|null` | 报名截止时间 |
| `qualifierEndTime` | `string\|null` | 资格赛截止时间 |

### myEntry（TournamentEntryDTO）

字段同 [entry.md 报名响应数据](./entry.md#1-报名)。

### MyCurrentMatchDTO（当前进行中的比赛）

| 字段 | 类型 | 说明 |
|---|---|---|
| `matchId` | `string` | 比赛bizId |
| `round` | `string` | 轮次 |
| `opponents` | `MatchOpponentDTO[]` | 对手信息（不含本人） |
| `courtBookerId` | `string\|null` | 订场人用户ID |
| `courtName` | `string\|null` | 球场名称 |
| `courtAddress` | `string\|null` | 球场地址 |
| `scheduledStartTime` | `string\|null` | 赛约开始时间 |
| `scheduledDuration` | `number\|null` | 赛约时长（小时） |
| `meetupId` | `string\|null` | 关联约球活动ID |
| `status` | `string` | 比赛状态，见 [match.md](./match.md) |
| `participants` | `MatchParticipantDTO[]` | 全部参与者（含本人） |

`MatchOpponentDTO`：`userId`/`nickname`/`avatarUrl`/`ntrpScore`

`MatchParticipantDTO`：`userId`/`teamId`/`confirmStatus`/`resultConfirmStatus`/`isWinner`

### actionState（待办状态枚举）

由后端根据 `myEntry.status` + `myCurrentMatch.status` + 是否为订场人 + 各方确认状态一次性计算，前端只需 switch-case 渲染，不需要自行拼装状态判断逻辑。

| 取值 | 说明 |
|---|---|
| `NOT_REGISTERED` | 未报名（或未登录） |
| `AWAIT_PAYMENT` | 待支付锁定正赛席位 |
| `AWAIT_COURT_BOOKER_SELECT` | 待选订场人 |
| `AWAIT_BOOKING` | 我是订场人，待提交场地时间 |
| `AWAIT_BOOKING_OPPONENT` | 对方订场中 |
| `AWAIT_SCHEDULE_CONFIRM` | 待我接受/打回重订/拒绝比赛 |
| `AWAIT_RESULT_SUBMIT` | 待提交谁赢了 |
| `AWAIT_RESULT_CONFIRM` | 待我确认结果/拒绝结果 |
| `WAITING_MATCH` | 排队等待下次匹配（或已操作，等对方响应） |
| `ELIMINATED` | 已被淘汰 |
| `WITHDRAWN` | 已主动退出 |
| `QUALIFIED_MAIN_DRAW` | 已获得正赛资格，正赛排队中 |

### myTimeline（个人事件流）

`TournamentTimelineEventDTO[]`：`time`/`description`，仅个人视角事件（报名成功、获得正赛资格、支付成功、匹配成功、确定订场人、提交赛约、提交比赛结果、比赛完成等），按时间正序排列。

### bracket（签表）

`TournamentBracketDTO`：`rounds: TournamentBracketRoundDTO[]`，按轮次顺序排列。

`TournamentBracketRoundDTO`：`round`/`matches: TournamentBracketMatchDTO[]`，同轮次内按 `matchNo` 排列。

`TournamentBracketMatchDTO`：`matchId`/`matchNo`/`participants: MatchOpponentDTO[]`/`winnerId`/`status`

### rejectRecords（拒绝比赛次数统计）

`TournamentRejectRecordDTO[]`：`userId`/`nickname`/`rejectCount`。统计赛事内所有报名者的拒绝比赛次数（资格赛+正赛累加），仅返回次数大于0的记录，不区分是否为本人。

**curl 示例**

已登录：
```bash
curl -X GET 'http://localhost:8080/api/rally/tournament/detail/T202608010001' \
  -H 'Authorization: Bearer <token>'
```

匿名（未登录，只返回公开区块）：
```bash
curl -X GET 'http://localhost:8080/api/rally/tournament/detail/T202608010001'
```
