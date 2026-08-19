# 赛事落地页详情（聚合查询）接口

**Base URL**: `/api/rally/tournament/detail`

只读聚合接口，赛事落地页收口为一个接口：聚合赛事信息、公开进程、当前用户报名与比赛、显式 `action`、个人时间线、签表、信用记录。前端只按 `action.state` switch-case 渲染"当前待办卡片"，不自行拼状态。

---

## 1. 赛事详情

**GET** `/{bizId}`

`userId` 从登录态取，**支持匿名访问**（未登录时 `action.state` 固定为 `NOT_LOGGED_IN`）。

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
| `action` | `TournamentActionDTO` | 待办状态及展示文案：`state`/`stateShow`/`stateTitle`/`stateSubtitle` |
| `restrictions` | `string[]\|null` | `NOT_REGISTERED` 返回报名限制，`FROZEN` 返回手机号限制，可叠加，文案由前端拼装 |
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
| `genderLimitShow` | `string` | 性别限制中文：不限/男子/女子 |
| `entryFee` | `number` | 报名费，单位：分 |
| `registrationStartTime` | `string` | 报名开始时间 |
| `registrationEndTime` | `string\|null` | 报名截止时间 |
| `qualifierStartTime` | `string` | 资格赛开始时间 |
| `qualifierEndTime` | `string\|null` | 资格赛截止时间 |
| `offlineFromRound` | `string` | 几强后转线下，枚举值：`ROUND_4`/`ROUND_8`/`ROUND_16` |
| `offlineFromRoundShow` | `string` | 转线下轮次中文 |
| `matchRuleDescription` | `string\|null` | 比赛规则描述，纯文本，支持 `\n` 换行 |
| `displayStatus` | `string` | 展示状态（基于时间计算）：`NOT_STARTED`/`REGISTRATION`/`IN_PROGRESS`/`ENDED`/`ABANDONED` |
| `displayStatusShow` | `string` | 展示状态中文：未开始/报名中/进行中/已结束/已废弃 |

### TournamentProgressDTO（公开进程）

| 字段 | 类型 | 说明 |
|---|---|---|
| `entryCount` | `number` | 报名人数（赛事所有报名记录总数） |
| `totalSlots` | `number` | 正赛总签位数 |
| `currentRound` | `string\|null` | 当前公开进行的轮次，尚无比赛时为 null |
| `currentRoundShow` | `string\|null` | 当前轮次中文 |
| `currentRoundTotalMatches` | `number` | 本轮总比赛场数 |
| `currentRoundCompletedMatches` | `number` | 本轮已完成场数 |
| `currentRoundAdvanceableSlots` | `number\|null` | 当前轮次可晋级名额：资格赛为正赛总签位数(totalSlots)，正赛为该轮签位数 |
| `currentRoundAdvancedCount` | `number\|null` | 当前轮次已晋级名额：按 entryNo 去重统计已进入下一轮的报名数 |
| `progressRate` | `number` | 总进度：资格赛按已晋级正赛人数 / totalSlots 计算，占 50%；正赛按已完成场次 / (totalSlots - 1) 计算，占 50% |
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
| `winnerEntryNo` | `integer\|null` | 本场待确认或已确认的获胜方报名编号 |
| `status` | `string` | 比赛状态，见 [match.md](./match.md) |
| `participants` | `MatchParticipantDTO[]` | 全部参与者（含本人） |

`MatchOpponentDTO`：`userId`/`nickname`/`avatarUrl`/`ntrpScore`

`MatchParticipantDTO`：`userId`/`nickname`/`avatarUrl`/`gender`/`phone`/`entryNo`/`confirmStatus`/`resultConfirmStatus`。`phone` 仅在当前用户查看自己的进行中比赛时，为不同 `entryNo` 的对手返回；本人和同队搭档不返回。

### action.state（待办状态枚举）

由后端根据 `myEntry.status` + `myCurrentMatch.status` + 是否为订场人 + 各方确认状态一次性计算，前端只需 switch-case 渲染，不需要自行拼装状态判断逻辑。

| 取值 | 说明 |
|---|---|
| `NOT_LOGGED_IN` | 未登录 |
| `NOT_REGISTERED` | 未报名，可检查报名限制 |
| `NOT_REGISTERED_CLOSED` | 报名已关闭 |
| `FROZEN` | 报名已冻结，绑定手机号后可解冻 |
| `AWAIT_QUALIFIER_START` | 报名成功，等待资格赛开始 |
| `AWAIT_PAYMENT` | 待支付锁定正赛席位 |
| `AWAIT_COURT_BOOKER_SELECT` | 待选择订场人 |
| `AWAIT_BOOKING` | 本人是订场人，待提交场地时间 |
| `AWAIT_BOOKING_REBOOK` | 赛约被打回，待重新提交 |
| `AWAIT_BOOKING_OPPONENT` | 对方订场中 |
| `AWAIT_SCHEDULE_CONFIRM` | 待本人确认赛约 |
| `AWAIT_OPPONENT_SCHEDULE_CONFIRM` | 本人已确认，等待对方确认赛约 |
| `AWAIT_PLAYING` | 尚未到约定开赛时间 |
| `AWAIT_RESULT_SUBMIT` | 待提交比赛结果 |
| `AWAIT_RESULT_CONFIRM` | 待本人确认结果 |
| `AWAIT_OPPONENT_RESULT_CONFIRM` | 本人已确认，等待其他人确认结果 |
| `WAITING_MATCH` | 等待匹配或等待对方后续操作 |
| `IN_OFFLINE_STAGE` | 已进入线下赛阶段 |
| `ELIMINATED` | 已被淘汰 |
| `WITHDRAWN` | 已主动退出 |
| `END` | 赛事已结束 |

当 `action.state=NOT_REGISTERED` 时，`restrictions` 可能包含：`NOT_LOGGED_IN`、`LEVEL_NOT_MATCH`、`PROFILE_INCOMPLETE`、`ONBOARDING_INCOMPLETE`、`REGISTRATION_INCOMPLETE`、`PHONE_MISSING`。列表为空时允许报名。当 `action.state=FROZEN` 时仅检查手机号，未绑定返回 `PHONE_MISSING`，已绑定返回空列表。

### myTimeline（个人事件流）

`TournamentTimelineEventDTO[]`：`time`/`description`，仅个人视角事件（报名成功、获得正赛资格、支付成功、匹配成功、确定订场人、提交赛约、提交比赛结果、比赛完成等），按时间正序排列。

### bracket（签表）

`TournamentBracketDTO`：`rounds: TournamentBracketRoundDTO[]`，按轮次顺序排列。

`TournamentBracketRoundDTO`：`round`/`roundShow`/`matches: TournamentBracketMatchDTO[]`，同轮次内按 `matchNo` 排列。

`TournamentBracketMatchDTO`：`matchId`/`matchNo`/`participants: MatchOpponentDTO[]`/`winnerEntryNo`/`status`

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
