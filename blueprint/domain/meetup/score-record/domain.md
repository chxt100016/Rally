---
id: "@meetup.score-record"
kind: aggregate
tables:
  - name: rally_meetup_score
    columns: [id, biz_id, rally_meetup_id, set_number, set_format, match_type, meetup_date, side_a_player1, side_a_player1_nickname, side_a_player1_avatar, side_a_player1_gender, side_a_player2, side_a_player2_nickname, side_a_player2_avatar, side_a_player2_gender, side_b_player1, side_b_player1_nickname, side_b_player1_avatar, side_b_player1_gender, side_b_player2, side_b_player2_nickname, side_b_player2_avatar, side_b_player2_gender, side_a_score, side_b_score, side_a_tiebreak_score, side_b_tiebreak_score, win_side, recorded_by, version, create_time, update_time]
---

## 概要

守护一场约球中一盘比分的阵容、计分、快照与版本一致。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 比分记录 | 比分业务编号 `biz_id` | 新增一盘时由雪花编号生成 | 约球、盘号、赛制、阵容快照、分数、胜方、记录人与版本 | `rally_meetup_score` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 球员快照 | 用户编号、昵称、头像资源键、性别 | 四个球员槽位及各自 `_nickname/_avatar/_gender` 列 |
| 双方阵容 | A/B 两侧各一至两位球员快照 | `side_a_*`、`side_b_*` |
| 盘比分 | 双方主分、可选抢七分、推导胜方 | `side_a_score`、`side_b_score`、两个 `tiebreak_score`、`win_side` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 约球引用 | `rally_meetup_id` | `@meetup.meetup` | 调用方传入可复盘阶段和有效参与者结论 |
| 球员/记录人引用 | 各球员编号、`recorded_by` | `@identity.user` | 保存发布时快照，不随用户资料变化回写 |

## 边界

一次加载与保存的单位是一条比分记录。阵容、球员快照、分数、胜方、比赛日期和版本必须整体变更；不同盘是独立聚合，以数据库唯一键保护同场盘号唯一。

约球参与资格、复盘阶段/截止时间、用户当前资料和后续评分重算都在边界外；调用方给出结论和快照后调用比分命令。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `RECORDED` | 一盘合法比分已记录，可在复盘期限内修正或删除 | `RECORDED`、`REMOVED` | `C2`、`C3` |
| `REMOVED` | 记录已物理删除，不再作为可加载聚合存在 | 无 | `C3` |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 比分编号非空唯一且建立后不变；盘号从 1 开始，同一约球盘号唯一 | 比分根、约球引用 | 盘号是本场比分顺序，检查和插入分离会产生两条互相竞争的同盘记录 | `SCORE_SET_DUPLICATE` |
| I2 | `SINGLE` 每侧恰一人；`DOUBLE` 每侧恰两人；`RALLY` 每侧一或两人且人数相等；所有球员非空、互不重复且属于外部有效参与者集合 | 双方阵容、球员引用 | 阵容与比赛类型分开保存会让一盘无法确定是单打还是双打，也无法可靠归因评分 | `SCORE_LINEUP_INVALID` |
| I3 | 每个阵容用户必须具有同一时点的昵称、头像键和合法性别快照，更新阵容时必须整体刷新对应快照 | 双方阵容、球员快照 | 用户编号与快照若分开更新，会出现新球员编号配旧球员姓名头像的历史错误 | `SCORE_PLAYER_SNAPSHOT_INVALID` |
| I4 | `GAME` 主分只接受胜方 6 且负方 0–4、7:5、7:6 或对称结果；7:6 时抢七分必填且胜方至少 7、领先至少 2，其他 GAME 结果抢七分为空 | 盘比分 | 主分、抢七分和胜方共同表达一盘结果，必须一次校验保存，不能出现平分或胜方与分数相反 | `SCORE_GAME_INVALID` |
| I5 | `TIEBREAK` 主分非负、胜方至少 7 且领先至少 2，附加抢七分字段必须为空；胜方始终由合法主分推导 | 盘比分 | 若允许直接传胜方或混用附加分，同一分数可被保存成两个相反结论 | `SCORE_TIEBREAK_INVALID` |
| I6 | 更新必须匹配当前版本，成功后版本原子加一；任何可修改字段与新版本同事务提交 | 比分根、全部值对象 | 只在内存比较版本无法阻止并发请求同时覆盖，快照与比分还可能来自不同版本 | `SCORE_VERSION_MISMATCH` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 新增一盘比分 | 尚不存在 | 约球可复盘结论、盘号、赛制、比赛类型、完整阵容与快照、分数、记录人、约球开始时间 | `RECORDED`，版本 0，胜方由分数推导 | 资格/期限不允许；违反 I1-I5；同场盘号并发冲突 |
| C2 | 修正一盘比分 | `RECORDED` | 约球可复盘结论、期望版本、完整新盘号/赛制/类型/阵容快照/分数、当前记录人和约球开始时间 | `RECORDED`，所有可编辑字段整体替换，版本加一 | 记录不属该约球；违反 I1-I6；新盘号冲突 |
| C3 | 删除一盘比分 | `RECORDED` 或目标不存在 | 约球可复盘结论、约球编号、比分编号 | `REMOVED`；目标不存在时幂等成功 | 操作人无有效报名或已过复盘期限；目标存在但属于其他约球 |

## 边界情况

- 主分相等、负分、0 盘号或跳号：平分/负分/0 盘号拒绝；盘号可不连续，只要求正数且同场唯一。
- SINGLE 带二号球员、DOUBLE 缺二号球员、两侧重复同一用户：拒绝。
- 用户资料不存在或不是本场有效参与者：拒绝，不以空快照降级保存。
- 7:6 未提供合法抢七分，或 6:4 却携带抢七分：拒绝。
- 同场同盘并发新增：一个成功，另一个返回 `SCORE_SET_DUPLICATE`，不按内容幂等。
- 两个更新使用同一版本：只有一个条件更新成功并递增版本，另一个返回 `SCORE_VERSION_MISMATCH`。
- 更新可空二号球员或抢七字段：采用完整替换语义，允许按新赛制明确清空旧值，不使用“忽略 null”的映射。
- 删除不存在或重复删除：幂等成功；删除不保留墓碑、删除人或原因。
- 用户资料后来变化：历史比分快照保持记录/最后修正时内容，不自动回写。

## 实现提示

更新 SQL 使用 `WHERE rally_meetup_id=? AND biz_id=? AND version=?` 并执行 `version=version+1`，必须检查影响行数。禁止用忽略 null 的通用更新器，四个球员槽位和抢七字段需要完整替换。`uk_meetup_set` 兜底同场盘号唯一；领域大写枚举统一映射到表值。
