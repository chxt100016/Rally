---
id: "@tour.match"
kind: aggregate
tables:
  - name: tour_match
    columns: [id, match_id, match_index, draw_id, tournament_id, year, round_number, round_name, player1_id, player2_id, winner_id, scheduled_at, scheduled_at_text, started_at, ended_at, court, court_seq, status, duration_minutes, description, match_date, sets_json, create_time, update_time]
---

## 概要

收敛职业巡回赛单场比赛的多来源快照。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 巡回赛比赛 | `draw_id+match_id` | 来源比赛号与已保存签表组合，数据库生成内部 id | 轮次、对阵、时间、场地、状态、胜方与盘分 | `tour_match` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 比赛身份 | 签表 id、来源比赛号、派生比赛序号 | `draw_id`、`match_id`、`match_index` |
| 轮次快照 | 轮次序号、轮次名称 | `round_number`、`round_name` |
| 对阵快照 | 双方球员、胜方 | `player1_id`、`player2_id`、`winner_id` |
| 排期快照 | 计划时间、排期原文、比赛日期、球场、场序 | `scheduled_at`、`scheduled_at_text`、`match_date`、`court`、`court_seq` |
| 进程快照 | 状态、开始/结束时间、时长、描述、各盘比分 | `status`、`started_at`、`ended_at`、`duration_minutes`、`description`、`sets_json` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 所属签表 | `draw_id` | `@tour.draw` | 只保存签表 id，不装载签表 |
| 来源赛事 | `tournament_id+year` | `@tour.tournament` | 保存查询冗余身份，不装载赛事 |
| 对阵球员 | `player1_id/player2_id/winner_id` | `@tour.player` | 允许尚未收录或尚未确定 |

## 边界

一次加载与保存的单位是一场由 `draw_id+match_id` 标识的比赛。签表、赛事和球员均在边界外；同一采集批次可依次保存多个比赛聚合，但其中一场的快照不与签表或其他比赛组成共同聚合。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `UNKNOWN` | 来源未给出可识别状态 | 任意状态 | `C1` |
| `PENDING` | 已知比赛但尚未临近开赛 | 任意状态 | `C1` |
| `COMING` | 临近开始 | 任意状态 | `C1` |
| `LIVE` | 正在比赛 | 任意状态 | `C1` |
| `FINISHED` | 来源标记已完成 | 任意状态 | `C1` |

状态是可纠正的来源快照，不是单向工作流；未知或空状态不会清除已有状态。

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | `draw_id+match_id` 唯一且建立后不可修改；同键补丁携带的赛事编号、年份不得与存量身份冲突 | 比赛根、比赛身份 | 身份漂移会把同一快照串到其他签表或赛事 | `TOUR_MATCH_IDENTITY_CONFLICT` |
| I2 | 更新只以来源非空字段覆盖；空字段保留存量，`sets_json` 非空时作为整体快照替换 | 比赛根、全部快照值对象 | 混合来源的补丁必须整体成功或整体不生效，避免半次刷新 | `TOUR_MATCH_SNAPSHOT_INVALID` |
| I3 | `match_index` 若存在，必须是从 `match_id` 数字部分派生的非负序号，不接受独立修改 | 比赛身份 | 派生索引与来源身份不一致会破坏排序与定位 | `TOUR_MATCH_INDEX_CONFLICT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 新增或刷新比赛快照 | 同键不存在或任意快照状态 | drawId、matchId、赛事身份及任意非空快照字段 | 新建比赛或原子合并补丁；状态可前进、回退或不变 | 关键身份缺失；身份冲突；派生序号冲突；快照格式非法 |

## 边界情况

- 不同签表可拥有相同 `match_id`，仍是不同比赛聚合。
- 来源字段为 null、状态未知或盘分无有效内容时保留存量；新建时相应字段可未知。
- 同批出现相同身份时按到达顺序合并非空字段，任何身份冲突使该批次保存失败。
- 来源遗漏既有比赛时不删除、不失效。
- 完赛数据可以被较旧来源回退，后续有效来源也可再次纠正，不施加状态单向约束。
- 签表已提交后比赛保存失败，不补偿删除签表；由后续采集再次补齐。

## 实现提示

`uk_tour_match_match_id` 保护 `match_id+draw_id` 自然键。写入前规范化空字符串为无值，按字段构造非空补丁；`sets_json` 不做局部盘合并。`tournament_id+year` 用于查询，但不得作为第二套可变身份。
