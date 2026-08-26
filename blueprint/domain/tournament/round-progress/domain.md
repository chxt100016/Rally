---
id: "@tournament.round-progress"
kind: service
reads:
  - name: rally_tournament
    columns: [biz_id, total_slots, current_filled_slots, current_round]
  - name: rally_tournament_match
    columns: [tournament_id, round, status]
---

## 概要

依据完赛场数与锁位进度判定赛事可推进的最远轮次。

## 职责

| 范围 | 内容 |
|---|---|
| 做什么 | 读取赛事总签位、已锁位数、当前轮次和各轮 COMPLETED 场数；计算当前快照下允许到达的最远轮次 |
| 不做什么 | 不修改赛事 currentRound；不推进或淘汰报名；不确认比赛结果；不锁定席位；不结束赛事或生成冠军 |
| 为什么不是聚合 | 判定需要跨一项赛事及多场比赛汇总完成数，结论不属于任一单场比赛聚合，服务自身也不持有状态 |
| 前置校验边界 | 输出只是基于读取快照的推进建议；真正写入必须由赛事聚合以“只向后”条件再次校验，跨报名淘汰由调用活动负责 |

## 契约

### 输入

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| tournamentId | 字符串 | 是 | 已存在赛事业务 id |

服务在同一读取快照中取得 `totalSlots`、`currentFilledSlots`、`currentRound`，并按轮次汇总状态为 COMPLETED 的比赛数。

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| decision | 枚举 | `NOT_READY`、`STAY` 或 `ADVANCE` |
| currentRound | 轮次 | 查询时赛事当前轮次 |
| targetRound | 轮次 | 按全部已完成事实推导的最远轮次；NOT_READY 时为空 |
| reason | 枚举 | `QUALIFIER_MATCHES_PENDING`、`MAIN_SLOTS_PENDING`、`ROUND_MATCHES_PENDING`、`ROUND_READY` 或 `FINAL_REMAINS` |
| evidence | 轮次计数列表 | 每项包含 round、requiredCompletedCount、actualCompletedCount |

赛事不存在或 totalSlots 不受支持时返回明确拒绝结论，不生成推进建议。

## 规则

R1 totalSlots 只接受 `2/4/8/16/32/64`；它同时决定正赛首轮：2→FINAL、4→ROUND_4、8→ROUND_8、16→ROUND_16、32→ROUND_32、64→ROUND_64。
R2 只统计状态精确为 COMPLETED 的比赛；REJECTED 和所有在途比赛不计入。某轮实际完成数大于阈值时仍视为完成。
R3 QUALIFIER 的所需完成场数等于 totalSlots。未达到时返回 NOT_READY，targetRound 为空，不检查后续轮次。
R4 资格赛场数已满足但 currentFilledSlots 为空或小于 totalSlots 时，targetRound=QUALIFIER；当前仍在资格赛则 STAY，不能提前进入正赛。
R5 资格赛完成且锁位数大于等于 totalSlots 时，从 R1 的正赛首轮开始逐轮检查；正赛轮次所需完成场数为该轮签位数除以 2。
R6 遇到第一个未完成的正赛轮次即停止；该轮之前最后一个可达轮次是 targetRound。若正赛首轮尚未完成，targetRound 仍是正赛首轮。
R7 轮次顺序固定为 `QUALIFIER→ROUND_64→ROUND_32→ROUND_16→ROUND_8→ROUND_4→FINAL`。输出只有晚于 currentRound 才是 ADVANCE；相等或更早一律 STAY，禁止回退。
R8 FINAL 完成后仍以 FINAL 为 targetRound 并给出 FINAL_REMAINS；本服务不推导赛事完成状态、冠军终态或 endTime。
R9 服务只读并返回判定，不得通过仓储更新 currentRound 或批量更新报名。

## 边界情况

- totalSlots 不在受支持集合：拒绝，不能猜测首轮。
- currentFilledSlots 为 null：按 0 处理，资格赛即使打满也保持 QUALIFIER。
- currentFilledSlots 大于 totalSlots：按已满处理，但不在本服务修复超占。
- 历史数据 currentRound 已晚于计算 targetRound：返回 STAY，不回退。
- 较早轮次未完成、较晚轮次却已有 COMPLETED 比赛：在首个缺口停止，不越级推进。
- FINAL 已完成：仍返回 FINAL，不修改赛事状态或报名冠军状态。
- 读取后又有比赛完成或席位变化：本次结论不补算，后续触发重新评估。

## 实现提示

查询只投影 frontmatter 列，并在一次一致性读取中按 round 聚合 COMPLETED 数量。把计算实现为无副作用函数，调用方收到 ADVANCE 后使用带轮次顺序条件的赛事聚合命令更新；现有 `TournamentRoundProgressService` 直接调用写仓储的部分应在实现阶段外移。
