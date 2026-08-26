---
id: "@tournament.match"
kind: aggregate
tables:
  - name: rally_tournament_match
    columns: [id, biz_id, tournament_id, match_no, round, group_size, court_booker_id, court_booker_selected_time, schedule_submitted_time, meetup_id, winner_entry_no, submitted_by, submitted_time, reject_phase, reject_reason_code, rejected_by, rejected_time, last_rebook_by, last_rebook_reason_code, last_rebook_time, status, matched_time, completed_time, version, create_time, update_time]
  - name: rally_tournament_match_participant
    columns: [id, biz_id, match_id, tournament_id, user_id, entry_no, confirm_status, confirm_time, result_confirm_status, result_confirm_time, create_time, update_time]
---

## 概要

守护赛事单场比赛从匹配到赛果完成的全过程。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 赛事比赛 | `biz_id`；自然键 `tournament_id+match_no` | 匹配时生成业务 id，并在赛事内分配 matchNo | 轮次、阶段、订场、赛约、赛果、拒绝审计与版本 | `rally_tournament_match` |

### 实体

| 名称 | 标识 | 生命周期 | 承载 | 表 |
|---|---|---|---|---|
| 比赛参与者 | `biz_id`；根内自然键 `user_id` | 随比赛创建，随根物理删除 | 报名编号、赛约确认、赛果确认及各自时间 | `rally_tournament_match_participant` |

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 比赛身份 | 赛事 id、展示编号、轮次、对阵单元数 | 根的 `tournament_id`、`match_no`、`round`、`group_size` |
| 订场委派 | 订场人、选定时间、赛约 id、提交时间 | 根的 `court_booker_id`、`court_booker_selected_time`、`meetup_id`、`schedule_submitted_time` |
| 赛果提交 | 胜方报名编号、提交人、提交时间 | 根的 `winner_entry_no`、`submitted_by`、`submitted_time` |
| 拒绝记录 | 阶段、理由、拒绝人、拒绝时间 | 根的 `reject_phase`、`reject_reason_code`、`rejected_by`、`rejected_time` |
| 最近重订 | 请求人、理由、时间 | 根的 `last_rebook_by`、`last_rebook_reason_code`、`last_rebook_time` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 所属赛事 | `tournament_id` | `@tournament.tournament` | 只保存赛事 id 与轮次快照 |
| 参赛报名 | `user_id+entry_no` | `@tournament.entry` | 参与者实体保存身份快照，不装载报名根 |
| 关联赛约 | `meetup_id` | `@meetup.meetup` | 场地、时间和费用以赛约为准 |

## 边界

一次加载与保存的单位是一场比赛根及其全部参与者实体。参与者确认与比赛阶段必须同成同败；报名、赛事和赛约仍是外部聚合，由活动事务协调其联动。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `MATCHED` | 已组成对阵，尚未选订场人 | `BOOKING/REJECTED` 或物理删除 | `C2/C8/C9` |
| `BOOKING` | 已选订场人，等待提交/重订赛约 | `SCHEDULED/REJECTED` 或物理删除 | `C3/C8/C9` |
| `SCHEDULED` | 赛约已提交，等待全员确认 | `SCHEDULED/BOOKING/PENDING_PLAY/REJECTED` | `C3/C4/C5/C8` |
| `PENDING_PLAY` | 赛约已确认，等待比赛与结果提交 | `PENDING_CONFIRM/REJECTED` | `C6/C8` |
| `PENDING_CONFIRM` | 结果已提交，等待全员确认 | `PENDING_CONFIRM/COMPLETED/REJECTED` | `C7/C8` |
| `COMPLETED` | 赛果已确认并完成 | `COMPLETED` | 无 |
| `REJECTED` | 比赛已终止 | `REJECTED` | 无 |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | `biz_id` 与 `tournament_id+match_no` 均唯一且建立后不可修改；matchNo 为正数，round 为受支持轮次 | 比赛根、比赛身份 | 比赛引用与赛事内展示编号不得漂移 | `TOURNAMENT_MATCH_IDENTITY_CONFLICT` |
| I2 | groupSize 只接受 2 或 3，参与者 userId 根内唯一；参与者的 distinct entryNo 数量必须等于 groupSize，胜方必须是其中一个 entryNo | 比赛根、全部参与者 | 对阵完整性与胜方合法性依赖同一份参与集合 | `TOURNAMENT_MATCH_PARTICIPANT_INVALID` |
| I3 | 订场人必须是参与者；进入 BOOKING 时同时设置选定时间，进入 SCHEDULED 时必须已有 meetupId 与提交时间 | 比赛根、订场委派、参与者 | 阶段与责任人/赛约事实不可半写 | `TOURNAMENT_MATCH_BOOKING_INVALID` |
| I4 | confirmStatus 与 resultConfirmStatus 分别只接受 `PENDING/CONFIRMED/REJECTED`，各自非 PENDING 时必须有对应时间 | 参与者实体 | 两条确认链必须独立且状态时间一致 | `TOURNAMENT_MATCH_CONFIRMATION_INVALID` |
| I5 | 进入 PENDING_CONFIRM 必须同时记录合法胜方、参与者提交人和提交时间；进入 COMPLETED 必须有胜方及完成时间 | 比赛根、赛果提交、参与者 | 赛果事实与生命周期推进必须原子提交 | `TOURNAMENT_RESULT_WINNER_REQUIRED` |
| I6 | 根及参与者的任何修改都必须以 version 条件保护，成功后 version 递增；版本不符不得部分保存 | 比赛根、全部参与者 | 订场认领、确认和超时任务存在并发竞争 | `TOURNAMENT_MATCH_VERSION_CONFLICT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 创建匹配比赛 | 不存在 | 赛事、轮次、matchNo、完整参与者、matchedTime、可选唯一订场人 | 有唯一订场人时 `BOOKING`，否则 `MATCHED`；两套确认均 PENDING | 身份重复；对阵不完整；订场人非法 |
| C2 | 认领订场人 | `MATCHED` | 参与者 userId、当前时间、version | `BOOKING` 并记录订场人/选定时间 | 非参与者；已被认领；版本冲突 |
| C3 | 提交或修改赛约 | `BOOKING/SCHEDULED` | 订场人、meetupId、提交时间、version | BOOKING 时转 `SCHEDULED`，订场人 CONFIRMED、其余 PENDING；SCHEDULED 修改保持确认 | 操作者/赛约不符；阶段非法；版本冲突 |
| C4 | 确认赛约 | `SCHEDULED` | 参与者 userId、确认时间、version | 本人 CONFIRMED；全员确认时 `PENDING_PLAY` | 非参与者；阶段非法；版本冲突 |
| C5 | 请求重订 | `SCHEDULED` | 参与者、重订理由与时间、version | `BOOKING`，记录最新重订并重置全员赛约确认为 PENDING | 非参与者；理由非法；版本冲突 |
| C6 | 提交赛果 | `PENDING_PLAY` | 参与者提交人、合法 winnerEntryNo、时间、version | `PENDING_CONFIRM`；提交人赛果确认 CONFIRMED，其余重置 PENDING | 非参与者；胜方不在本场；版本冲突 |
| C7 | 确认或自动完成赛果 | `PENDING_CONFIRM` | 确认参与者或超时事实、确认时间、version | 按需更新确认；全员已确认或超时完成时 `COMPLETED` | 缺胜方；非参与者；版本冲突 |
| C8 | 拒绝比赛或赛果 | 任意非终态 | 可选拒绝阶段、理由、拒绝人、时间、version | `REJECTED`，按场景更新发起人确认 | 场景前置条件/超时/限额不满足；版本冲突 |
| C9 | 取消未订场比赛 | `MATCHED/BOOKING` | 运营取消意图、version | 条件物理删除根及全部参与者 | 状态已变化；删除条件未命中；版本冲突 |

## 边界情况

- 恰有一个可订场参与者的匹配可直接创建为 BOOKING；否则从 MATCHED 等待首次认领。
- BOOKING 提交赛约会重置赛约确认；SCHEDULED 内只修改赛约资料时保留已确认结果。
- 请求重订保留订场人、原 meetupId 和原提交时间，只覆盖最近一次重订记录并重置赛约确认。
- 赛约重复确认可刷新本人时间；全员确认后即进入表注释遗漏但流程必需的 PENDING_PLAY。
- 提交赛果会清除其他参与者残留的赛果确认；超时自动完成只把仍 PENDING 的确认补为 CONFIRMED。
- 用户拒绝、超时或退赛可把任意在途比赛终止为 REJECTED；退赛场景允许拒绝审计字段为空。
- 运营物理取消严格限于 MATCHED/BOOKING，SCHEDULED 及以后禁止删除。
- 终止或删除后的报名释放、草稿赛约关闭以及完成后的报名结算属于跨聚合应用事务。

## 实现提示

两个唯一键分别保护业务 id 和赛事内编号，`uk_match_user` 保护根内参与者唯一。仓储必须整聚合加载并批量保存参与者；所有阶段命令使用 version 条件更新根，受影响行数为 0 即冲突。补齐状态枚举中的 `PENDING_PLAY`，不要只依赖旧数据库列注释。
