---
id: "@meetup.score-record"
kind: aggregate
tables:
  - name: rally_meetup_score
    columns: [id, biz_id, rally_meetup_id, set_number, set_format, match_type, meetup_date, side_a_player1, side_a_player1_nickname, side_a_player1_avatar, side_a_player1_gender, side_a_player2, side_a_player2_nickname, side_a_player2_avatar, side_a_player2_gender, side_b_player1, side_b_player1_nickname, side_b_player1_avatar, side_b_player1_gender, side_b_player2, side_b_player2_nickname, side_b_player2_avatar, side_b_player2_gender, side_a_score, side_b_score, side_a_tiebreak_score, side_b_tiebreak_score, win_side, recorded_by, version, create_time, update_time]
---

## 概要

保存约球单盘比分、球员展示快照与胜方。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 比分记录 | 比分业务编号 `biz_id` | 新增一盘时由雪花编号生成 | 约球、盘号、赛制、原始阵容字段、可选展示快照、分数、胜方、记录人与数据库版本字段 | `rally_meetup_score` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 球员展示快照 | 用户编号，以及批量查询命中时取得的昵称、头像资源键、性别；查询未命中时展示字段可空 | 四个球员槽位及各自 `_nickname/_avatar/_gender` 列 |
| 双方阵容字段 | A/B 两侧一号球员及可选二号球员；领域不校验人数结构、重复或参与资格 | `side_a_*`、`side_b_*` |
| 盘比分 | 双方主分、原样保存的可选抢七分、仅由主分大小推导的胜方 | `side_a_score`、`side_b_score`、两个 `tiebreak_score`、`win_side` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 约球引用 | `rally_meetup_id` | `@meetup.meetup` | 调用方在进入比分服务前完成复盘资格、阶段和期限校验 |
| 球员/记录人引用 | 各球员编号、`recorded_by` | `@identity.user` | 球员不要求存在或属于本场；查询命中时保存展示快照，后续不回写 |

## 边界

一次插入、更新或删除的单位是一条比分记录。不同盘独立保存，以数据库唯一键保护同场盘号唯一。

约球参与资格、复盘阶段/截止时间、用户资料查询和后续评分重算都在边界外。main 的更新先读记录并在内存比较版本，随后按 `meetupId+bizId` 更新非空字段；版本不进入 SQL 条件、不会递增，可空字段不能显式清除。该弱并发与部分更新行为属于必须保留的既有业务事实。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `RECORDED` | 一盘比分已记录，可在调用方允许复盘时修正或删除 | `RECORDED`、`REMOVED` | `C2`、`C3` |
| `REMOVED` | 记录已物理删除，不再作为可加载聚合存在 | 无 | `C3` |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 新增前按 `meetupId+setNumber` 预检不存在；同场盘号最终唯一由 `uk_meetup_set` 兜底。盘号不要求为正数或连续 | 比分根、约球引用 | 与 main 的预检和数据库约束一致 | 预检重复为 `SCORE_SET_DUPLICATE`；并发唯一冲突自然成为系统错误 |
| I2 | 双方主分不得相等；胜方只由主分较大一侧推导。负分、非网球计分及抢七字段组合不额外校验 | 盘比分 | 保持 main 的 `TennisScorePolicy` 行为 | `INVALID_WIN_SIDE` |
| I3 | 阵容字段按请求原样保存；不校验比赛类型对应人数、重复球员、用户存在性或本场参与资格。用户查询未命中时快照为空仍允许保存 | 双方阵容、球员快照 | 保留 main 的宽松录入和资料补充语义 | 无专用拒绝 |
| I4 | 更新先读取 `meetupId+bizId`，不存在拒绝；只在内存比较读取版本与请求版本。实际更新不带版本条件、不递增版本且仅写非空字段 | 比分根、全部字段 | 精确描述 main 的弱乐观锁和 MyBatis-Plus 默认更新行为 | `RECAP_SCORE_NOT_FOUND`、`SCORE_VERSION_MISMATCH` |
| I5 | 删除按 `meetupId+bizId` 物理删除，不预读；命中一条或零条均成功 | 比分根 | 保留重复删除及跨约球编号零行成功 | 无专用拒绝 |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 新增一盘比分 | 尚不存在 | 约球编号、盘号、赛制、比赛类型、原始阵容、分数、记录人、约球开始时间，以及查询命中的球员快照 | `RECORDED`，数据库默认版本 0，胜方由主分大小推导 | 同场盘号预检重复；双方主分相等；保存失败 |
| C2 | 修正一盘比分 | `RECORDED` | 约球编号、比分编号、读取版本、新盘号/赛制/类型/阵容/分数、当前记录人、约球开始时间和查询命中的快照 | `RECORDED`，按 `meetupId+bizId` 更新非空字段；版本不递增 | 记录不存在；读取版本与请求不等；双方主分相等；数据库更新失败 |
| C3 | 删除一盘比分 | `RECORDED` 或目标不存在 | 约球编号、比分编号 | `REMOVED`；目标不存在、已删或属于其他约球时零行也成功 | 删除执行失败 |

## 边界情况

- 主分相等拒绝；负分、0/负盘号、跳号、非网球计分与任意抢七字段组合均可保存。
- SINGLE 带二号球员、DOUBLE/RALLY 缺二号球员、两侧重复用户均不额外拒绝。
- 用户资料不存在或不是本场参与者仍可进入阵容；未命中的展示快照为空。
- 同场同盘并发新增：一个成功，另一个由数据库唯一键产生系统失败，不转换为专用业务码。
- 两个更新使用同一版本可同时通过内存比较并互相覆盖；版本保持不变。
- 更新中的 null 不清除旧二号球员、抢七字段或快照；新用户资料缺失时可能保留旧快照。
- 删除不存在或重复删除：幂等成功；删除不保留墓碑、删除人或原因。
- 用户资料后来变化：历史比分快照保持记录/最后修正时内容，不自动回写。

## 实现提示

保留现有仓储：新增先预检同场盘号，随后普通 insert；更新先读并在内存比版本，随后以 `WHERE rally_meetup_id=? AND biz_id=?` 使用默认非空字段更新，不检查影响行数、不递增版本；删除同条件物理删除且不检查影响行数。`uk_meetup_set` 仅作为并发兜底，约束异常自然按系统错误传播。
