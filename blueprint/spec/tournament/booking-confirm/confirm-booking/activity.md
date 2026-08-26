---
id: tournament.booking-confirm.activity.confirm-booking
depends_on: []
reads:
  - name: rally_meetup
    columns: [biz_id, start_time, status]
---

## 概要

记录本人赛约确认，并在全员确认后开放比赛与草稿赛约。

## 时序图

```mermaid
sequenceDiagram
    participant U as 比赛参与者
    participant A as confirm-booking 活动
    participant M as @tournament.match
    participant E as @tournament.entry
    participant P as @meetup.meetup
    U->>A: matchId/confirm=true
    A->>M: 校验 SCHEDULED 与参与身份
    A->>P: 校验关联赛约尚未开始
    A->>M: 确认本人并按需进入 PENDING_PLAY
    A->>P: 全员确认时按需开放草稿赛约
    A-->>U: 成功
```

## 触发条件

登录参与者对 SCHEDULED 比赛提交 `confirm=true` 时执行。

## 活动契约

关联赛约存在时必须尚未到开始时间；过期则不修改。校验通过后将本人确认状态覆盖为 CONFIRMED 并刷新时间；尚有未确认者保持 SCHEDULED，全员确认则进入 PENDING_PLAY，并尝试把关联 DRAFT 赛约改为 OPEN。

## 异常分支

| 错误标识 | 触发条件 | 处理 |
|---|---|---|
| `TOURNAMENT_ENTRY_NOT_FOUND` | 比赛、本人报名不存在或不在参与者中 | 事务回滚 |
| `TOURNAMENT_NOT_FOUND` | 所属赛事不存在 | 事务回滚 |
| `TOURNAMENT_INVALID_SCHEDULE_CONFIRM` | 比赛不是 SCHEDULED | 不修改 |
| `MEETUP_EXPIRED` | 关联赛约开始时间已经过去 | 不确认本人，不修改比赛、参与关系或赛约 |
| `TOURNAMENT_MATCH_VERSION_CONFLICT` | 并发修改比赛 | 事务回滚，刷新重试 |
| `OPERATION_FAILED` | 比赛、参与关系或赛约保存失败 | 整体回滚，不交付部分确认 |

## 领域依赖

### @tournament.match
- 输入：比赛、当前参与者与版本
- 输出：确认状态及比赛阶段更新

### @tournament.entry
- 输入：本人报名身份
- 输出：参与资格确认

### @meetup.meetup
- 输入：关联 meetupId、开始时间与当前时间
- 输出：开始前允许确认；全员确认时 DRAFT 赛约按需开放

## 业务动作

A1 校验比赛与参与身份
A2 校验关联赛约尚未开始
A3 覆盖本人确认状态
A4 判定全员确认并推进比赛
A5 按需开放草稿赛约

## 详细流程

1. 读取比赛、参与者、所属赛事和本人报名，要求比赛为 SCHEDULED 且本人属于参与者。
2. `confirm=true` 且关联赛约记录存在时，要求其 startTime 晚于当前时间；开始时间已经过去则返回 MEETUP_EXPIRED，本次不修改任何对象。未关联赛约或记录缺失时保留原兼容行为。
3. 本人无论原为 PENDING、CONFIRMED 或异常 REJECTED，均覆盖为 CONFIRMED 并刷新确认时间。
4. 尚有未确认者时比赛保持 SCHEDULED；全员确认时改为 PENDING_PLAY。
5. 全员确认且关联赛约存在并为 DRAFT 时改为 OPEN；编号空、赛约缺失或非 DRAFT 不阻止比赛推进。
6. 以比赛版本条件在同一事务保存比赛、参与关系与可变赛约，失败整体回滚。

## 边界情况

- 重复确认会刷新本人确认时间，并重新判断全员状态。
- 过期校验只作用于 confirm=true；拒赛和申请重订仍允许处理已经开始的赛约。
- 赛约无法满足开放条件时比赛仍可进入 PENDING_PLAY。
- 成功只返回 data=null，不发送本活动通知。

## 实现提示

活动读取关联赛约的 startTime；一致性由 `@tournament.match`、`@tournament.entry` 与 `@meetup.meetup` 协作表达。
