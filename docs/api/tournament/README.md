# 赛事（Tournament）接口文档

赛事域接口分三个模块，均以 `/api/rally` 为统一前缀：

| 模块 | Base URL | 说明 | 文档 |
|---|---|---|---|
| 赛事管理（运营后台） | `/tournament/admin` | 创建/编辑/激活/废弃/列表 | [admin.md](./admin.md) |
| 赛事报名（用户端） | `/tournament/entry` | 报名/修改偏好/退出 | [entry.md](./entry.md) |
| 比赛流程（用户端） | `/tournament/match` | 订场人认领/提交场地/确认赛约/提交结果/确认结果 | [match.md](./match.md) |
| 落地页详情（聚合查询） | `/tournament/detail` | 只读聚合：赛事信息/进程/我的报名与比赛/actionState/时间线/签表/信用记录 | [detail.md](./detail.md) |

## 渠道说明

- Web 通用接口前缀如上表。
- 微信小程序渠道额外提供以下前缀（入参/返参与对应 web 接口完全一致）：
  - `/wechat/tournament/entry/*`
  - `/wechat/tournament/match/*`

## 通用响应结构

所有接口统一返回 `Result<T>` 包装：

```json
{
  "code": 0,
  "message": null,
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `number` | `0` 表示成功，非 0 表示业务错误 |
| `message` | `string\|null` | 错误信息，成功时为 null |
| `data` | 对应接口数据 | 无返回数据的接口为 `null` |

## 赛事域错误码

| code | 说明 |
|---|---|
| 46001 | 赛事不存在 |
| 46002 | 赛事当前状态不允许该操作 |
| 46003 | 赛事配置不完整 |
| 46004 | 赛事时间点设置不合法 |
| 46005 | 报名记录不存在 |
| 46006 | 您已报名该赛事 |
| 46007 | 报名未开放或已截止 |
| 46008 | 报名当前状态不允许该操作 |
| 46009 | 正赛退出退款功能暂未开放，请联系客服 |
| 46010 | 比赛状态已变更，请刷新后重试（乐观锁冲突，前端应刷新页面重新拉取最新状态） |
| 46011 | 订场人已被选定 |
| 46012 | 只有待选订场人才能认领 |
| 46013 | 只有订场人可以提交场地信息 |
| 46014 | 当前状态不允许确认赛约 |
| 46015 | 当前状态不允许提交结果 |
| 46016 | 当前状态不允许确认结果 |
| 46017 | 已达拒绝次数上限 |
| 46018 | 无效的拒绝理由 |
| 46019 | 请提供打回重订理由 |
| 46020 | 请提供赛约时间 |
| 46021 | 请选择获胜方 |

## 核心枚举

### TournamentStatusEnum（赛事状态）
`DRAFT` 草稿 / `ACTIVE` 进行中 / `ABANDONED` 已废弃

### TournamentGenderLimitEnum（性别限制）
`ALL` / `MALE` / `FEMALE`

### TournamentEntryStageEnum（报名阶段）
`QUALIFY` 资格赛 / `MAIN` 正赛

### TournamentEntryStatusEnum（报名状态）
`WAITING` 排队匹配 / `IN_MATCH` 比赛中 / `PAYING` 待支付（仅 QUALIFY 阶段） / `ELIMINATED` 淘汰 / `WITHDRAWN` 主动退出

### TournamentRoundEnum（轮次）
`QUALIFIER` / `ROUND_32` / `ROUND_16` / `ROUND_8` / `ROUND_4` / `FINAL`

### CourtAbilityEnum（场地能力）
`CAN_BOOK` 能订场 / `CANNOT_BOOK` 不能订场

### TournamentMatchStatusEnum（比赛状态）
`MATCHED` 已匹配 / `BOOKING` 订场中 / `SCHEDULED` 已约定场地待确认 / `PENDING_PLAY` 待打 / `PENDING_CONFIRM` 待确认结果 / `COMPLETED` 已完成 / `REJECTED` 已终止

### ConfirmStatusEnum（确认状态，赛约确认/结果确认共用）
`PENDING` / `CONFIRMED` / `REJECTED`

### CourtSelectModeEnum（球场选择模式，与约球域一致）
`TEXT` 文本搜索 / `MAP` 地图选择 / `FREE` 自由输入

### ScheduleRejectReasonEnum（拒绝比赛理由）
`TIME_PLACE_CONFLICT` 时间/场地实在协调不了 / `DONT_WANT_PLAY` 不想打了 / `OTHER` 其他

### RebookReasonEnum（打回重订理由）
`TIME_NOT_SUITABLE` 时间不合适 / `PLACE_NOT_SUITABLE` 地点不合适 / `OTHER` 其他

### ResultRejectReasonEnum（拒绝结果理由）
`DISPUTE_APPEAL` 不服，我要申诉重来 / `OPPONENT_LEVEL_MISMATCH` 对手水平明显超出本赛事等级 / `RESULT_INCORRECT` 提交的结果不属实 / `OTHER` 其他
