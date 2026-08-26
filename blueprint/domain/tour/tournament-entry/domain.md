---
id: "@tour.tournament-entry"
kind: aggregate
tables:
  - name: tour_tournament_entry
    columns: [id, player_id, draw_id, seed, entry_type, status, create_time, update_time]
---

## 概要

维护职业球员在一个签表中的参赛身份与资格。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 签表参赛项 | `draw_id+player_id` | 已保存签表与外部球员编号组合，数据库生成内部 id | 种子、入围方式与参赛状态 | `tour_tournament_entry` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 参赛身份 | 签表 id、外部球员编号 | `draw_id`、`player_id` |
| 参赛资格 | 种子号、入围方式 | `seed`、`entry_type` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 所属签表 | `draw_id` | `@tour.draw` | 只保存签表 id，不装载签表 |
| 职业球员 | `player_id` | `@tour.player` | 结合所属签表的 tour 解释球员身份 |

## 边界

一次加载与保存的单位是一个球员在一个签表中的参赛项。签表和球员资料均在边界外；同一球员进入不同签表时形成不同聚合。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `CONFIRMED` | 已确认进入签表 | `CONFIRMED/WITHDRAWN/RETIRED` | `C1/C2` |
| `WITHDRAWN` | 明确退赛 | `WITHDRAWN` | `C2` |
| `RETIRED` | 明确因退赛结束参赛 | `RETIRED` | `C2` |

采集来源遗漏或字段为空不会触发状态迁移；退出状态只能由明确命令设置。

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | `draw_id+player_id` 唯一且建立后不可修改 | 参赛根、参赛身份 | 身份漂移会把种子和状态挂到另一签表或球员 | `TOUR_ENTRY_IDENTITY_CONFLICT` |
| I2 | `entry_type` 只接受 `DIRECT/WILDCARD/QUALIFIER/LUCKY_LOSER`；seed 若有不得为负，0 表示来源明确但不作为种子展示 | 参赛根、参赛资格 | 资格字段必须在同一次保存中保持可解释 | `TOUR_ENTRY_QUALIFICATION_INVALID` |
| I3 | `WITHDRAWN/RETIRED` 只能由明确退出命令产生，采集补丁不得修改状态，退出后不再由采集恢复 | 参赛根 | 防止不完整来源或后到快照逆转明确退出事实 | `TOUR_ENTRY_STATUS_CONFLICT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 新增或刷新参赛资格 | 同身份不存在或 `CONFIRMED` | drawId、playerId、可选 seed/entryType | 新建 `CONFIRMED`，或非空合并资格且状态不变 | 身份缺失/冲突；资格值非法；已退出 |
| C2 | 记录明确退出 | `CONFIRMED` | `WITHDRAWN` 或 `RETIRED` 及明确退出意图 | 对应终态 | 目标状态非法；已退出为其他状态 |

## 边界情况

- `seed=null` 表示来源未知，`seed=0` 表示来源给出但不作为种子展示；两者都允许记录存在。
- 采集补丁的 null 不清空旧 seed/entryType，来源遗漏也不删除、不标记退出。
- 同批重复身份按到达顺序合并非空资格；身份冲突使本批保存失败。
- 无参赛附加信息时调用方可不发命令；不会据此创建空记录。
- 已提交签表、比赛和球员资料不因参赛批次失败而回滚。

## 实现提示

`uk_tour_entry_player_draw_year` 实际保护 `draw_id+player_id`。采集实现只调用 `C1` 并限制更新列为 seed/entry_type；退出必须走独立 `C2`。按 playerId 查询职业球员时还需从签表取得 tour，避免跨巡回赛歧义。
