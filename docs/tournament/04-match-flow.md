# 模块 4：比赛流程（状态机）

## 职责
承载一场比赛从匹配成功到完成/终止的完整状态机。全部操作收口在比赛落地页的"当前待办卡片"，不跳独立页面。含订场人选择、订场、赛约确认、结果提交确认，以及超时轮询处理。

## 状态机
```
MATCHED(24h) → BOOKING(48h) → SCHEDULED(48h) → 待比赛 → PENDING_CONFIRM(48h) → COMPLETED
                                                                              ↘ REJECTED（终止，全员回池）
```

## 聚合根 / 领域对象
- **TournamentMatch（聚合根）+ MatchParticipant（聚合内实体）**。
  - Match 承载订场人、球场、赛约时间、结果、拒绝/打回信息、status、各时间戳、version。
  - Participant 承载各人 confirmStatus / resultConfirmStatus / isWinner / teamId。
  - 乐观锁：订场人抢占、结果提交按 version 条件更新；participant 表无并发竞争，不需要锁。

## 领域 Service 能力（TournamentMatchFlowService）
每个动作一个方法，内部做状态校验（Assert 当前 status 合法）+ 状态流转 + 参与者状态更新，app 层不 try-catch。
- `selectCourtBooker(matchId, userId)` / `giveUpCourtBooker`：MATCHED 抢订场人（乐观锁），或 BOOKING 放弃退回 MATCHED 重新计时。
- `submitBooking(cmd, userId)`：订场人提交赛约，BOOKING → SCHEDULED，写 scheduleSubmittedTime。**订场即按约球全量数据创建草稿约球（meetupType=TOURNAMENT、status=DRAFT），match 仅存 meetupId 关联，场地/时间/费用等均以约球为准（比赛表不再冗余存这些字段）。返回 meetupId。** 后续修改场地/时间由订场人跳转约球活动页编辑。
- `handleScheduleConfirm(matchId, userId, action, reasonCode?, text?)`：非订场人处理赛约。
  - 接受：置本人 confirmStatus=CONFIRMED；全员 CONFIRMED → 草稿约球激活（DRAFT → OPEN）→ 待比赛。
  - 打回重订：退回 BOOKING、重置所有 confirmStatus=PENDING、记 last_rebook_*，不计拒绝、无上限。
  - 拒绝比赛：REJECTED，写 reject_phase=SCHEDULE_REJECT，全员回池，拒绝方 +1 拒绝次数（按 stage 判上限），草稿约球关闭（DRAFT → CLOSED）。
- `submitResult(matchId, userId, winnerTeamId)`：待比赛→PENDING_CONFIRM，乐观锁防并发，提交人自动 resultConfirmStatus=CONFIRMED，不填比分。
- `handleResultConfirm(matchId, userId, action, reasonCode?, text?)`：
  - 确认：本人 CONFIRMED；全员 CONFIRMED → COMPLETED，写 winnerId，按 teamId 置 isWinner，胜者晋级（QUALIFY 胜者进 PAYING 待支付 / MAIN 胜者进下一轮 WAITING，交模块 5/晋级逻辑），败者 ELIMINATED。
  - 拒绝结果：REJECTED，reject_phase=RESULT_REJECT，全员回池，拒绝方 +1。
- 拒绝上限控制：达上限的用户在对应阶段隐藏"拒绝"选项，仅保留"确认/打回重订"。

## 接口清单

### `POST /tournament/match/court-booker`
选择/放弃订场人身份。入参 matchId + action（SELECT/GIVE_UP）。乐观锁防双方同抢，失败提示前端刷新。

### `POST /tournament/match/book`
提交赛约。入参与发布约球（MeetupPublishCmd）一致的全量约球字段 + matchId + tournamentId。仅订场人、仅 BOOKING 态。订场即创建草稿约球并返回 meetupId。

### `POST /tournament/match/schedule-confirm`
处理赛约。入参 matchId + action（ACCEPT/REBOOK/REJECT）+ 可选 rebookReasonCode/rejectReasonCode + 可选 text。理由预设见源文档；终止性拒绝必须选理由。

### `POST /tournament/match/submit-result`
提交结果。入参 matchId + winnerTeamId（按 teamId 分组，双打选队、单打选人）。乐观锁，后到者提示"对方已提交"。

### `POST /tournament/match/result-confirm`
处理结果。入参 matchId + action（CONFIRM/REJECT）+ 可选 rejectReasonCode/text。

## 超时判定 Job（内部）
每 2 小时轮询（不用延时队列），按 status 用对应时间戳判超时：MATCHED→matchedTime(24h)、BOOKING→courtBookerSelectedTime 或 lastRebookTime(48h)、SCHEDULED→scheduleSubmittedTime(48h)、PENDING_CONFIRM→submittedTime(48h)。命中后按规则处理：MATCHED/BOOKING 超时视为拒绝比赛终止并记未响应；SCHEDULED/PENDING_CONFIRM 超时未操作方默认接受/同意。误差最多晚 2 小时，可接受。

## 与其他模块的边界
- 匹配产出的 Match 从此进入本模块。
- 创建 Meetup 调约球域（解耦，约球只管场地/时间，胜负在此判）。
- COMPLETED 的晋级动作触发模块 5（资格→正赛支付引导）或下一轮回池（模块 3）。
- REJECTED 使参与者回 WAITING，成为模块 3 下次匹配候选。
