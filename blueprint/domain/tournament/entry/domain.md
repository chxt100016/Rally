---
id: "@tournament.entry"
kind: aggregate
tables:
  - name: rally_tournament_entry
    columns: [id, biz_id, tournament_id, user_id, partner_id, entry_no, preferred_districts, court_ability, available_times, stage, status, current_round, qualifier_reject_count, main_draw_reject_count, qualified_time, paid_time, last_visit_time, create_time, update_time]
---

## 概要

守护用户在一项业余赛事中的报名与晋级状态。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 赛事报名 | `biz_id`；自然键 `tournament_id+user_id` | 报名时生成业务 id，并在赛事内分配 entryNo | 搭档、匹配偏好、赛段、状态、轮次、拒绝次数及关键时间 | `rally_tournament_entry` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 报名身份 | 赛事 id、用户 id、报名编号、可选搭档用户 id | `tournament_id`、`user_id`、`entry_no`、`partner_id` |
| 匹配偏好 | 地区集合、订场能力、可比赛时间集合 | `preferred_districts`、`court_ability`、`available_times` |
| 晋级进度 | 赛段、当前轮次、预留资格时间、支付时间 | `stage`、`current_round`、`qualified_time`、`paid_time` |
| 拒绝计数 | 资格赛次数、正赛次数 | `qualifier_reject_count`、`main_draw_reject_count` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 所属赛事 | `tournament_id` | `@tournament.tournament` | 赛事配置、轮次和限额在边界外 |
| 报名用户 | `user_id` | `@identity.user` | 只保存用户业务 id |
| 双打搭档 | `partner_id` | 另一 `@tournament.entry` 的用户 | 搭档报名是独立聚合，由注册活动协调反向关系 |

## 边界

一次加载与保存的单位是一个用户在一个赛事中的报名。双打搭档、比赛、赛事和支付单均在边界外；共享 entryNo、两边搭档关系及同场多人状态由应用事务协调，不把多个报名根合成一个聚合。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `WAITING` | 位于当前轮次匹配池 | `FROZEN/IN_MATCH/ELIMINATED/WITHDRAWN` | `C3/C4/C8/C9` |
| `FROZEN` | 暂停匹配 | `WAITING/WITHDRAWN` | `C3/C9` |
| `IN_MATCH` | 已被一场进行中比赛占用 | `WAITING/PAYING/ELIMINATED/WITHDRAWN` | `C5/C6/C9` |
| `PAYING` | 资格赛胜出，等待锁定正赛席位 | `WAITING/WITHDRAWN` | `C7/C9` |
| `ELIMINATED` | 已被赛事淘汰 | `ELIMINATED` | 无 |
| `WITHDRAWN` | 用户主动退出 | `WITHDRAWN` | 无 |

`PAYING→WAITING` 必须与 `QUALIFY→MAIN`、首轮设置和 paidTime 同时发生；终态不可恢复或重新报名。

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | `biz_id` 与 `tournament_id+user_id` 均唯一且建立后不可修改；entryNo 为正数且建立后不可修改 | 报名根、报名身份 | 身份或编号漂移会破坏支付关联、搭档组队与历史比赛定位 | `TOURNAMENT_ENTRY_IDENTITY_CONFLICT` |
| I2 | partnerId 不得等于 userId，设置后不得换绑；共享 entryNo 与反向搭档由注册事务跨两个报名根校验 | 报名根、报名身份 | 防止一个报名同时落入多个双打队伍 | `TOURNAMENT_PARTNER_ALREADY_PAIRED` |
| I3 | 地区和可比赛时间均至少一项，courtAbility 只接受 `CAN_BOOK/CANNOT_BOOK`；三组偏好必须整组替换 | 报名根、匹配偏好 | 匹配算法只能消费一份同版本、完整的约束集合 | `TOURNAMENT_ENTRY_PREFERENCE_INVALID` |
| I4 | 赛段只接受 `QUALIFY/MAIN`，轮次只接受 `QUALIFIER/ROUND_64/ROUND_32/ROUND_16/ROUND_8/ROUND_4/FINAL`；MAIN 不得处于 QUALIFIER，QUALIFY 不得处于正赛轮次 | 报名根、晋级进度 | 状态、赛段和轮次必须原子迁移，避免进入错误匹配池 | `TOURNAMENT_ENTRY_PROGRESS_INVALID` |
| I5 | 两类拒绝次数均非负且只在对应赛段拒赛成功时加一；达到赛事配置限额时拒绝整条拒赛命令 | 报名根、拒绝计数 | 次数和比赛拒绝结果必须同成同败，不能超限后再补偿 | `TOURNAMENT_REJECT_LIMIT_REACHED` |
| I6 | 当前命令不得臆造 qualifiedTime；paidTime 只随 PAYING 成功晋级 MAIN 首次设置；lastVisitTime 不得被更早时刻覆盖 | 报名根、晋级进度、访问时间 | 未启用字段与已发生的支付、访问事实必须清晰区分 | `TOURNAMENT_ENTRY_TIME_CONFLICT` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 创建资格赛报名 | 同赛事用户无任意状态旧报名 | 用户、赛事、正数 entryNo、可选搭档、完整偏好 | `QUALIFY/WAITING/QUALIFIER`，计数为 0 | 重复报名；搭档冲突；偏好非法 |
| C2 | 整组替换匹配偏好 | 非 `ELIMINATED/WITHDRAWN` | 地区集合、courtAbility、时间集合 | 状态与进度不变，三组偏好一起替换 | 任一组缺失或格式非法；报名已终止 |
| C3 | 冻结或解冻 | `WAITING` 或 `FROZEN` | 明确目标状态 | `WAITING→FROZEN` 或 `FROZEN→WAITING` | 来源状态不精确；重复操作 |
| C4 | 锁入比赛 | `WAITING` 且轮次与赛事当前轮次一致 | matchId 所代表的匹配意图 | `IN_MATCH` | 已冻结/占用/终止；轮次不一致 |
| C5 | 释放回匹配池 | `IN_MATCH` | 比赛拒绝、超时或未订场取消事实；可选本赛段拒绝计数递增 | `WAITING`，按需把对应计数加一 | 非在赛；计数已达限额；赛段非法 |
| C6 | 结算比赛结果 | `IN_MATCH` | 胜负、是否决赛及完成时间 | 资格胜方 `PAYING`、资格负方 `WAITING`；正赛胜方 `WAITING` 并按需晋级，负方 `ELIMINATED` | 结果不明确；赛段/轮次非法 |
| C7 | 锁定正赛席位 | `QUALIFY/PAYING` | paidTime、由总签位映射的正赛首轮 | `MAIN/WAITING/首轮` | 非待支付；首轮映射非法；席位未原子占用 |
| C8 | 淘汰未晋级报名 | `QUALIFY/WAITING` | 资格赛完成且正赛席位已满事实 | `ELIMINATED` | 条件未满足；非资格等待报名 |
| C9 | 主动退赛 | 任意非终态 | 当前用户退赛意图 | `WITHDRAWN`，其他字段保留 | 已 `WITHDRAWN/ELIMINATED`；报名不存在 |
| C10 | 记录赛事访问 | 任意状态 | 本次访问时间 | 状态不变，保留较晚 lastVisitTime | 时间缺失；不得以更早时间回退 |

## 边界情况

- `WITHDRAWN/ELIMINATED` 旧报名仍阻止同用户重新报名。
- 双打搭档可先有单边报名；新成员复用其 entryNo，并在应用事务中补齐双方 partnerId。
- 比赛拒绝、超时或未订场取消时，只把仍为 IN_MATCH 的报名释放到 WAITING，其他状态跳过。
- 资格赛胜方先进入 PAYING；取得预支付参数不改变报名，只有有效支付推进才进入 MAIN。
- `ROUND_64` 是 64 签正赛首轮的合法值；旧报名表注释遗漏它，不作为领域限制。
- `qualified_time` 当前没有引用活动写入，保持空值；不得用结果确认时间或支付时间代填。
- 支付回调、恢复和到期任务可能形成订单已付但报名未推进的部分状态；重复推进仍必须要求 PAYING，防止重复改轮次。
- 冻结只允许 WAITING，解冻只允许 FROZEN，重复调用均返回状态非法。
- 冻结、比赛中或待支付报名仍可改未来匹配偏好；终态不可修改偏好。
- 详情访问是读取链路中的独立写入，后续详情拼装失败不回滚已记录时间。

## 实现提示

`uk_tournament_user` 和 `uk_biz_id` 双重保护身份。所有状态更新带当前状态条件；匹配释放只更新 IN_MATCH。偏好 JSON 在领域边界解析、校验后整组序列化。支付推进、赛果结算及双打绑定需要应用事务协调其他聚合，但不得放宽本聚合的迁移前置条件。
