---
id: "@tournament.unmatched-entry-elimination"
kind: service
reads: []
---

## 概要

判定指定用户在赛事当前轮是否允许被单独淘汰。

## 职责

| 范围 | 内容 |
|---|---|
| 做什么 | 根据赛事当前轮次、指定用户的报名快照及其是否参与进行中比赛，输出该用户是否允许被淘汰的判定 |
| 不做什么 | 不读取或保存赛事、报名和比赛；不改变报名状态；不扫描或联动其他报名；不校验或淘汰双打搭档；不产生运营响应或通知 |
| 为什么不是聚合 | 判定需要组合赛事轮次、一个报名聚合快照与跨多场比赛汇总出的参与事实，没有单个报名聚合能够独立取得全部上下文，服务本身也不持有状态 |
| 前置校验边界 | 输出只是基于调用方输入快照的预检结论；真正淘汰前仍须由调用活动锁定指定报名、复核赛事状态和进行中比赛，并由报名聚合及条件持久化保证最终一致性 |

## 契约

### 输入

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| `tournamentCurrentRound` | 赛事轮次 | 是 | 本次允许淘汰的赛事当前轮次。 |
| `entry` | 目标报名快照 | 是 | 只包含请求 `userId` 对应报名的 `userId`、`status`、`currentRound`。 |
| `inActiveMatch` | 布尔值 | 是 | 指定用户是否参与本赛事状态为 `MATCHED/BOOKING/SCHEDULED/PENDING_PLAY/PENDING_CONFIRM` 的比赛。 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `decision` | 判定 | `ELIGIBLE`：允许淘汰；`ENTRY_STATUS_OR_ROUND_INVALID`：报名状态或轮次不符合；`IN_ACTIVE_MATCH`：用户仍参与进行中比赛；`INPUT_INVALID`：必填上下文缺失。 |

## 规则

R1 `tournamentCurrentRound`、`entry` 或 `inActiveMatch` 任一为 null 时，返回 `INPUT_INVALID`。
R2 `entry.userId` 为空时，返回 `INPUT_INVALID`。
R3 `entry.currentRound` 不等于 `tournamentCurrentRound`，或 `entry.status` 不是 `WAITING/FROZEN` 时，返回 `ENTRY_STATUS_OR_ROUND_INVALID`。
R4 `inActiveMatch` 为 true 时，返回 `IN_ACTIVE_MATCH`。
R5 通过 R1 至 R4 后返回 `ELIGIBLE`。
R6 服务为纯计算，不修改输入对象，不访问仓储，不生成业务编号，也不产生通知或其他副作用。

## 边界情况

- 已进入 `COMPLETED/REJECTED` 比赛的历史参与不属于进行中比赛，不阻止淘汰。
- 双打场景中，`partnerId`、`entryNo` 与搭档报名状态不参与判定；本服务只判定请求 `userId`，不会联动搭档。
- 报名处于 `IN_MATCH/ELIMINATED/QUIT` 等非候选状态时，统一返回 `ENTRY_STATUS_OR_ROUND_INVALID`。
- `inActiveMatch` 事实由调用活动按指定赛事与用户查询；本服务不纠正调用方传入的错误赛事范围。

## 实现提示

使用无状态领域服务按规则顺序返回单一枚举判定；调用活动只在结果为 `ELIGIBLE` 时调用报名聚合的单人淘汰命令。
