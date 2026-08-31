---
id: tournament.result-confirm-admin.activity.confirm-result-by-admin
depends_on: []
reads: []
---

## 概要

按赛事和比赛序号一次性代确认全部待确认参与者赛果，完成比赛并结算胜负方报名。

## 时序图

```mermaid
sequenceDiagram
    participant F as confirm-result 流程
    participant A as confirm-result-by-admin 活动
    participant T as @tournament.tournament
    participant M as @tournament.match
    participant E as @tournament.entry
    F->>A: tournamentId + matchNo
    A->>T: 确认赛事存在
    A->>M: 按自然键锁定读取最新比赛及参与者
    alt 比赛不是 PENDING_CONFIRM 或无胜方
        M-->>A: 拒绝代确认
    else 满足前置条件
        loop 每个仍为 PENDING 的参与者
            A->>M: 确认赛果（confirm=true）
        end
        A->>M: 以当前版本统一保存
        alt 全员已 CONFIRMED
            A->>E: 按胜负逐一结算参赛报名
            A-->>F: 比赛已完成，交付轮次与胜方信息
        else 仍有参与者停留在 REJECTED
            A-->>F: 代确认成功，比赛保持待确认
        end
    end
```

## 触发条件

运营流程已完成请求字段校验，对由 `tournamentId+matchNo` 唯一指定、状态为 `PENDING_CONFIRM` 且已有胜方参赛编号的一场比赛发起代确认时执行。

## 活动契约

### 入参

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| `tournamentId` | 字符串 | 是 | 已通过非空校验。 |
| `matchNo` | 整数 | 是 | 正整数。 |

### 成功返回

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `matchCompleted` | 布尔 | 是 | 本次代确认后比赛是否已进入 `COMPLETED`；仍有参与者停留在 `REJECTED` 时为 `false`。 |
| `tournamentId` | 字符串 | 否 | `matchCompleted=true` 时给出，供轮次推进使用。 |
| `round` | 轮次 | 否 | `matchCompleted=true` 时给出已完成比赛的轮次。 |
| `winnerEntryNo` | 整数 | 否 | `matchCompleted=true` 时给出胜方参赛编号。 |
| `completedTime` | 日期时间 | 否 | `matchCompleted=true` 时给出比赛完成时间。 |

## 异常分支

| 错误标识 | 触发条件 | 来源 |
|---|---|---|
| `TOURNAMENT_NOT_FOUND` | 指定赛事不存在 | confirm-result 流程 `TOURNAMENT_NOT_FOUND` 一行 |
| `TOURNAMENT_MATCH_NOT_FOUND` | 指定赛事下不存在该比赛序号 | confirm-result 流程 `TOURNAMENT_MATCH_NOT_FOUND` 一行 |
| `TOURNAMENT_INVALID_RESULT_CONFIRM` | 比赛不是 `PENDING_CONFIRM` | confirm-result 流程 `TOURNAMENT_INVALID_RESULT_CONFIRM` 一行 |
| `TOURNAMENT_RESULT_WINNER_REQUIRED` | 比赛为 `PENDING_CONFIRM` 但没有已记录的胜方参赛编号 | confirm-result 流程 `TOURNAMENT_RESULT_WINNER_REQUIRED` 一行 |
| `TOURNAMENT_ENTRY_NOT_FOUND` | 全员确认后，任一胜方或负方参与者没有对应赛事报名 | confirm-result 流程 `TOURNAMENT_ENTRY_NOT_FOUND` 一行 |
| `TOURNAMENT_MATCH_VERSION_CONFLICT` | 保存前比赛已被并发修改 | confirm-result 流程 `TOURNAMENT_MATCH_VERSION_CONFLICT` 一行 |
| `OPERATION_FAILED` | 比赛、参与关系或报名本次变化未能完整保存 | confirm-result 流程 `OPERATION_FAILED` 一行 |

## 领域依赖

### @tournament.tournament

- 输入：赛事编号与确认赛事存在的意图。
- 输出：赛事存在时返回赛事身份；不存在时返回失败结论。

### @tournament.match

- 输入：按 `tournamentId+matchNo` 自然键锁定读取最新根及全部参与者的意图；比赛为 `PENDING_CONFIRM` 且已有胜方参赛编号时，对每个赛果确认状态仍为 `PENDING` 的参与者依次发起确认赛果命令（`confirm=true`），最终以当前版本统一保存。
- 输出：正常时返回代确认后各参与者最新确认状态，以及比赛是否已全员确认并进入 `COMPLETED`（含胜方参赛编号、轮次和完成时间）；异常时给出比赛缺失、状态非 `PENDING_CONFIRM`、无胜方或版本冲突结论。

### @tournament.entry

- 输入：比赛已进入 `COMPLETED` 时，对本场每一名参与者按其对应报名，发起结算比赛结果命令，传入胜负关系、比赛轮次和完成时间。
- 输出：正常时按赛段和轮次返回各报名的结算后状态（资格赛胜方 `PAYING`/负方 `WAITING`；非决赛正赛胜方晋级 `WAITING`/负方 `ELIMINATED`；决赛胜方 `CHAMPION`/负方 `ELIMINATED`）；异常时给出对应报名缺失结论。

## 业务动作

- A1 确认指定赛事存在。
- A2 按赛事编号和比赛序号锁定并加载最新比赛聚合及全部参与者，确认比赛为 `PENDING_CONFIRM` 且已有胜方参赛编号。
- A3 对每个赛果确认状态仍为 `PENDING` 的参与者，依次在同一内存聚合上覆盖为 `CONFIRMED` 并刷新确认时间；已被记为 `REJECTED` 的参与者保持原状，不参与本次覆盖。
- A4 以当前版本统一保存比赛根与全部参与关系；全员确认时比赛进入 `COMPLETED` 并记录完成时间。
- A5 比赛进入 `COMPLETED` 后，按胜负方结算对应参赛报名。

## 详细流程

1. `A1` 只确认赛事身份存在，不要求赛事状态。
2. `A2` 按自然键取得比赛根与全部参与者；查不到报比赛不存在；比赛不是 `PENDING_CONFIRM` 或没有已记录的胜方参赛编号时拒绝代确认，不修改任何对象。
3. `A3` 逐个覆盖仍为 `PENDING` 的参与者为 `CONFIRMED` 并记录确认时间；已是 `CONFIRMED` 的参与者保持原状；已被记为 `REJECTED` 的参与者保持原状，不覆盖。
4. `A4` 全部参与者确认状态凑齐为 `CONFIRMED` 后，以版本条件将比赛改为 `COMPLETED` 并记录完成时间；条件未命中报版本冲突；仍有参与者停留在 `REJECTED` 时比赛保持 `PENDING_CONFIRM`，不进入 A5。
5. `A5` 仅在比赛进入 `COMPLETED` 时执行：按胜方参赛编号找出胜方与负方参与者，分别结算各自对应的赛事报名；任一参与者没有对应报名时报告报名不存在。
6. `A3` 至 `A5` 在同一事务内完成；任一必要保存失败整体回滚。

## 边界情况

- 比赛为 `PENDING_CONFIRM` 但参与者已全部 `CONFIRMED` 时按幂等处理，不重复改写确认时间，不重复结算报名，`matchCompleted` 仍返回 `true` 但不重复调用结算命令。
- 代确认后仍有参与者停留在 `REJECTED`，无法凑齐全部 `CONFIRMED` 时，本次代确认按成功返回，`matchCompleted=false`，比赛保持 `PENDING_CONFIRM`，不结算报名、不推进轮次；待该参与者的拒绝被其他方式处理后可再次代确认。
- 双打同队参与者共享参赛编号，但各自独立拥有赛果确认状态，按参与者逐人处理，不做队内一致性提示。
- 本活动不发起报名费支付、不占用正赛席位，资格赛胜方仅进入 `PAYING`。
- 成功不发送通知，不记录运营操作人。

## 实现提示

在同一次内存加载的聚合上连续对每个待确认参与者调用确认赛果命令，最终统一以当前 `version` 做一次条件保存；仓储加载方式与 `tournament.booking-confirm-admin.activity.confirm-booking-by-admin` 一致。轮次推进与赛事终态交由 `tournament.result-confirm-admin.activity.advance-tournament-progress` 处理，本活动只负责比赛完成与报名结算。
