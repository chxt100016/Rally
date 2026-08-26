---
id: "@tournament.tournament"
kind: aggregate
tables:
  - name: rally_tournament
    columns: [id, biz_id, tournament_name, poster_key, rule_poster_key, wechat_group_qr_code_key, match_type, city_code, city_name, ntrp_level, gender_limit, total_slots, offline_from_round, offline_meetup_id, qualifier_group_size, entry_fee, prize_money, registration_start_time, registration_end_time, qualifier_start_time, qualifier_end_time, end_time, qualifier_reject_limit, main_draw_reject_limit, match_rule_description, ext_data, status, current_filled_slots, current_round, champion_entry_no, create_time, update_time]
---

## 概要

守护业余赛事配置、生命周期、签位和轮次进度。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 业余赛事 | `biz_id` | 创建草稿时生成雪花业务 id | 展示、准入、赛制、费用、时间、限额、生命周期、签位和轮次 | `rally_tournament` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 展示配置 | 名称、三项图片资源键、规则说明、主题扩展 | `tournament_name`、`poster_key`、`rule_poster_key`、`wechat_group_qr_code_key`、`match_rule_description`、`ext_data` |
| 参赛准入 | 城市编码/名称、NTRP、性别限制 | `city_code`、`city_name`、`ntrp_level`、`gender_limit` |
| 赛制配置 | 单双打、总签位、线下起始轮次、资格赛组人数 | `match_type`、`total_slots`、`offline_from_round`、`qualifier_group_size` |
| 费用与奖项 | 报名费、按名次排列的奖金 | `entry_fee`、`prize_money` |
| 时间窗口 | 报名起止、资格赛起止、实际结束时间 | `registration_start_time`、`registration_end_time`、`qualifier_start_time`、`qualifier_end_time`、`end_time` |
| 拒绝限额 | 资格赛上限、正赛上限 | `qualifier_reject_limit`、`main_draw_reject_limit` |
| 运行进度 | 已锁定正赛席位、当前轮次、冠军报名编号、线下活动 id | `current_filled_slots`、`current_round`、`champion_entry_no`、`offline_meetup_id` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 展示图片 | 三项资源键 | `@media.asset-storage` | 只保存对象键，不装载或删除资源 |
| 城市名录 | `city_code+city_name` | `@system.location-catalog` | 创建或换城前解析，聚合保存成对快照 |
| 线下赛事活动 | `offline_meetup_id` | `@meetup.meetup` | 只允许条件绑定一次 |

## 边界

一次加载与保存的单位是一项业余赛事。报名、比赛、支付单和线下约球属于其他聚合；赛事根只维护自身配置与进度，跨聚合创建、淘汰、退款和通知由活动事务协调。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `DRAFT` | 配置中的未开放赛事 | `ACTIVE/ABANDONED` | `C3/C4` |
| `ACTIVE` | 已开放报名、匹配和比赛 | `FINISHED/ABANDONED` | `C4/C8` |
| `FINISHED` | 决赛完成且冠军已记录的终态 | `FINISHED` | 无 |
| `ABANDONED` | 运营废弃的终态 | `ABANDONED` | 无 |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | bizId 唯一且建立后不可修改；名称非空且不超过 128 字符 | 赛事根、展示配置 | 所有报名、比赛和支付都依赖稳定赛事身份 | `TOURNAMENT_IDENTITY_CONFLICT` |
| I2 | matchType 只接受 `SINGLE/DOUBLE`；totalSlots 为 2 到 64 的 2 次方；qualifierGroupSize 只接受 2 或 3 | 赛事根、赛制配置 | 匹配规模、正赛首轮和参与者结构必须由同一合法配置推导 | `TOURNAMENT_CONFIG_INCOMPLETE` |
| I3 | offlineFromRound 可空，空表示全程线上；非空时必须是受支持轮次且其 slotCount 严格小于 totalSlots；offlineMeetupId 只能在 currentRound 等于它时从空绑定一次 | 赛事根、赛制配置、运行进度 | 明确线上赛制并防止在线阶段未到即创建或并发绑定多个线下活动 | `TOURNAMENT_OFFLINE_ROUND_INVALID` |
| I4 | cityCode 非空且必须与 location-catalog 返回的 cityName 成对保存；NTRP 可解析，genderLimit 只接受 `ALL/MALE/FEMALE` | 赛事根、参赛准入 | 准入与展示必须使用同一城市和规范限制 | `TOURNAMENT_ELIGIBILITY_INVALID` |
| I5 | entryFee、两类拒绝上限及每项奖金均不得为负；奖金按名次顺序作为完整列表替换 | 赛事根、费用与奖项、拒绝限额 | 收费、奖励和拒赛判断必须在同一配置版本下可解释 | `TOURNAMENT_AMOUNT_INVALID` |
| I6 | 报名开始严格早于资格赛开始；报名截止不得早于报名开始；资格赛截止不得早于资格赛开始；endTime 若有不得早于资格赛开始 | 赛事根、时间窗口 | 所有准入、匹配和结束判断依赖一致时间轴 | `TOURNAMENT_TIME_ILLEGAL` |
| I7 | currentFilledSlots 始终在 0..totalSlots；占位只能以 `< totalSlots` 条件原子加一，不允许通用配置命令覆盖 | 赛事根、运行进度 | 防止并发支付超卖或运营更新擦除已锁席位 | `TOURNAMENT_SLOTS_FULL` |
| I8 | currentRound 初始为 QUALIFIER，只能沿 `QUALIFIER→ROUND_64→ROUND_32→ROUND_16→ROUND_8→ROUND_4→FINAL` 向后推进且不得被配置命令覆盖 | 赛事根、运行进度 | 并发赛果与支付建议不能让赛事越级回退 | `TOURNAMENT_ROUND_CONFLICT` |
| I9 | 只有 round=FINAL 且 status=COMPLETED 的比赛事实才能结束 ACTIVE 赛事；FINISHED 必须同时具有 championEntryNo 和 endTime，冠军编号等于该决赛 winnerEntryNo | 赛事根、运行进度 | 仅处于 FINAL 阶段不代表已决出冠军，冠军、结束时间和终态必须原子一致 | `TOURNAMENT_RESULT_WINNER_REQUIRED` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 创建赛事草稿 | 不存在 | bizId、完整合法配置及解析后的 cityName；offlineFromRound 可空 | `DRAFT/QUALIFIER`，锁位 0，championEntryNo/endTime/offlineMeetupId 为空 | 配置、金额、时间、城市或身份非法 |
| C2 | 更新赛事配置 | `DRAFT/ACTIVE/FINISHED/ABANDONED` | 完整配置意图；offlineFromRound 的 null 表示全程线上，其他可选 null 保留旧值 | 状态与全部运行进度不变，仅原子替换合法配置 | 赛事不存在；配置无效；换城未同步名称；试图覆盖进度 |
| C3 | 激活赛事 | `DRAFT` | 运营激活意图 | `ACTIVE`，其他字段不变 | 非草稿；核心时间缺失或次序非法 |
| C4 | 废弃赛事 | `DRAFT/ACTIVE` | 运营废弃意图 | `ABANDONED`，配置和进度保留 | 已废弃；赛事不存在 |
| C5 | 原子锁定正赛席位 | `ACTIVE` 且锁位未满 | 首次有效报名支付事实 | currentFilledSlots 加一，状态/轮次不变 | 已满；重复业务推进；赛事非 ACTIVE |
| C6 | 单向推进当前轮次 | `ACTIVE` | `@tournament.round-progress` 给出的较晚目标轮次 | currentRound 更新到目标 | 目标无效、相等或更早时空操作；赛事非 ACTIVE |
| C7 | 绑定线下赛事活动 | currentRound 等于 offlineFromRound 且尚未绑定 | meetupId | 仅设置 offlineMeetupId | 轮次未到；已绑定或并发先绑定；编号为空 |
| C8 | 完成赛事 | `ACTIVE` | 已完成决赛的 winnerEntryNo、completedTime | `FINISHED`，championEntryNo=winnerEntryNo，endTime=completedTime，currentRound 保持 FINAL | 非 ACTIVE；比赛非 FINAL/COMPLETED；冠军或完成时间缺失 |

## 边界情况

- totalSlots 支持 `2/4/8/16/32/64`；旧表注释只列 16/32/64，不作为领域限制。
- offlineFromRound 为空时全部轮次在线上完成；非空时沿用完整轮次枚举且 slotCount 必须小于 totalSlots。
- 配置更新允许发生在 DRAFT/ACTIVE/FINISHED/ABANDONED，但不得改变 status、currentRound、currentFilledSlots、championEntryNo、endTime 或 offlineMeetupId。
- 配置更新可选字段传 null 时保留旧值；奖金非空时整组替换，不做逐名次合并。
- 变更 cityCode 必须重新解析并一起替换 cityName，禁止保留旧城市名。
- 重复激活、重复废弃均不是幂等成功；较旧轮次推进建议则按条件空操作。
- 支付事实已发生但锁位失败时不能回滚外部付款；本聚合仍严格拒绝超位，由支付恢复流程处理不一致。
- 废弃 ACTIVE 赛事只改变根状态，不自动关闭报名、比赛、支付或线下活动。
- currentRound=FINAL 只表示赛事处于决赛阶段；必须由已完成决赛触发 C8，不能仅凭 currentRound 产生冠军。

## 实现提示

配置更新使用显式白名单 DTO，绝不映射运行进度列；城市变更先调用 location-catalog。锁位 SQL 带 `current_filled_slots < total_slots` 条件并检查影响行数，轮次更新使用固定枚举顺序条件。决赛完成、冠军报名结算和赛事 C8 由同一事务协调。线下绑定使用 `offline_meetup_id IS NULL` 条件更新以收敛并发。
