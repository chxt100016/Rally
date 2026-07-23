# 模块 6：落地页详情（聚合查询）

## 职责
赛事落地页收口为**一个详情接口**，聚合赛事信息、公开进程、当前用户报名与比赛、显式 actionState、个人时间线、签表、信用记录。前端只按 actionState switch-case 渲染"当前待办卡片"，不自行拼状态。此模块只读，不改状态。

## 聚合根 / 领域对象（只读装配）
- 读 **Tournament**（基础信息 + 席位进度 + 当前轮次概览）。
- 读当前用户 **TournamentEntry**（myEntry）。
- 读当前用户进行中的 **TournamentMatch + Participant**（myCurrentMatch）。
- 读约球域（Meetup 场地/时间，用于 myCurrentMatch 展示）、用户域（对手昵称等，用 userId 回查）。

## 领域 Service 能力（TournamentDetailService / actionState 计算）
- `assembleDetail(tournamentId, userId)`：并行装配各区块，userId 为空则只返回公开部分（tournament + progress）。
- **actionState 计算**（核心）：由后端根据 myEntry.status + myCurrentMatch.status + 是否订场人 + 各方确认状态**一次性算出**，取值见源文档枚举：NOT_REGISTERED / AWAIT_PAYMENT / AWAIT_COURT_BOOKER_SELECT / AWAIT_BOOKING / AWAIT_BOOKING_OPPONENT / AWAIT_SCHEDULE_CONFIRM / AWAIT_RESULT_SUBMIT / AWAIT_RESULT_CONFIRM / WAITING_MATCH / ELIMINATED / WITHDRAWN / QUALIFIED_MAIN_DRAW。
- progress 计算：currentFilledSlots/totalSlots、当前公开轮次、本轮总场数/已完成场数。
- timeline：仅个人视角事件流（不含他人操作），从报名起逐条：匹配成功、订场、赛约确认、打回、约球完成、结果提交/确认、支付、晋级、申诉等。
- creditRecords：仅与我相关的申诉/未响应记录，措辞轻量化（"申诉了N次"，不用负面表述；打回重订不进信用记录）。
- bracket：签表对阵图数据。

## 接口清单

### `GET /tournament/detail/{bizId}`
核心聚合接口。userId 从 UserContext 取（可匿名，匿名只返回公开区块）。返回 `TournamentDetailDTO`：
- `tournament`：基础信息。
- `progress`：公开进程（席位、轮次、本轮场数、各截止时间）。
- `myEntry`：报名信息，未报名为 null。
- `myCurrentMatch`：进行中比赛（matchId/round/opponents/球场/时间/meetupId/status/participants），无则 null。
- `actionState`：显式待办状态，驱动待办卡片渲染。
- `myTimeline`：个人事件流。
- `bracket`：签表。
- `myCreditRecords`：与我相关的轻量化信用记录。

三种视角由 actionState + myEntry 决定：未报名（赛事信息+席位进度+立即报名）、已报名进行中（待办卡片+对手+对应操作按钮）、已淘汰（战绩汇总+查看完整赛程）。

## 与其他模块的边界
- 纯读聚合，所有写操作在模块 2/4/5；本模块只反映状态。
- actionState 是前后端契约核心：新增/变更比赛状态时须同步更新此计算逻辑。
- 依赖约球域、用户域、支付域做展示数据回查，不产生副作用。
