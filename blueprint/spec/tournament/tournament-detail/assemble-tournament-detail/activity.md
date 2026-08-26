---
id: tournament.tournament-detail.activity.assemble-tournament-detail
depends_on: []
reads:
  - name: rally_tournament
    columns: [biz_id, tournament_name, poster_key, rule_poster_key, wechat_group_qr_code_key, match_type, city_code, city_name, ntrp_level, gender_limit, total_slots, offline_from_round, offline_meetup_id, qualifier_group_size, entry_fee, prize_money, registration_start_time, registration_end_time, qualifier_start_time, qualifier_end_time, end_time, qualifier_reject_limit, main_draw_reject_limit, match_rule_description, ext_data, status, current_filled_slots, current_round, champion_entry_no]
  - name: rally_tournament_entry
    columns: [biz_id, tournament_id, user_id, partner_id, entry_no, preferred_districts, court_ability, available_times, stage, status, current_round, qualifier_reject_count, main_draw_reject_count, last_visit_time]
  - name: rally_tournament_match
    columns: [biz_id, tournament_id, match_no, round, group_size, court_booker_id, court_booker_selected_time, schedule_submitted_time, meetup_id, winner_entry_no, submitted_by, submitted_time, reject_phase, reject_reason_code, rejected_by, rejected_time, last_rebook_by, last_rebook_reason_code, last_rebook_time, status, matched_time, completed_time, version]
  - name: rally_tournament_match_participant
    columns: [match_id, tournament_id, user_id, entry_no, confirm_status, confirm_time, result_confirm_status, result_confirm_time]
  - name: rally_meetup
    columns: [biz_id, meetup_type, creator_id, title, match_type, max_players, current_players, city_code, city_name, district_name, start_time, end_time, duration, court_name, court_address, court_lng, court_lat, court_select_mode, court_id, level_mode, level_min, level_max, gender_limit, join_mode, cost_data, status]
  - name: rally_meetup_chat_user
    columns: [ref_id, user_id, unread_count]
  - name: user
    columns: [user_id, nickname, avatar_url, gender, phone]
  - name: user_tennis_profile
    columns: [user_id, ntrp_score, status]
  - name: rally_court
    columns: [biz_id, name, type, surface]
---

## 概要

聚合赛事公开信息、个人进度、当前动作与关联活动卡片。

## 时序图

```mermaid
sequenceDiagram
    participant C as 匿名/登录访问者
    participant A as assemble-tournament-detail 活动
    participant DB as 赛事、报名、比赛及展示资料
    C->>A: tournamentId 与可选 userId
    A->>DB: 聚合公开资料和比赛进度
    A->>DB: 按身份补个人、对手、赛约与未读
    A->>A: 推导动作并签名图片
    A-->>C: 裁剪后的赛事详情
```

## 触发条件

匿名或登录用户打开指定赛事详情时执行；记录访问后会再次补全个人区块。

## 活动契约

赛事必须存在。匿名返回含冠军在内的公开区块，登录未报名补报名动作，已报名补本人进度、时间线、未读、当前比赛、对手及赛约；冠军显示 CHAMPION，已进入下一轮但赛事尚未推进时显示 ADVANCED；图片地址有效一小时。

## 异常分支

| 错误标识 | 触发条件 | 处理 |
|---|---|---|
| `TOURNAMENT_NOT_FOUND` | 赛事不存在 | 不交付 |
| 降级 | 未登录/未报名、当前比赛或资料缺失、对手无法唯一定位 | 裁剪或留空后成功 |
| 登录凭证无效 | 限制判断所需本人资料不存在 | 终止；先前访问记录可能保留 |
| `MEETUP_NOT_FOUND` | 记录的关联活动不存在 | 终止；访问记录不回滚 |
| `OPERATION_FAILED` | 转换、图片签名或依赖读取失败 | 终止；访问记录可能保留 |

## 领域依赖

无

## 业务动作

A1 聚合赛事与整体进度
A2 按身份裁剪个人区块
A3 补当前比赛和对手信息
A4 推导当前动作与活动卡片
A5 补用户资料并签名图片

## 详细流程

1. 读取赛事、全部报名、比赛和参与关系，组装公开资料、展示状态、进度、签表、参赛者和拒赛统计。
2. 匿名仅公开区块和 NOT_LOGGED_IN；登录未报名给报名动作及个人限制，不创建报名。
3. 已报名者补本人报名、时间线、评论未读；IN_MATCH 时取当前未完成/终止比赛，缺失则按等待匹配展示。
4. 聚合参与者、确认进度、对手访问时间；只有本人参赛编号可唯一定位且关系允许时交付对手手机号，订场阶段补对手偏好。
5. 根据赛事时间、报名轮次、赛事当前轮次、当前比赛和赛约推导动作：CHAMPION 优先于赛事 END，WAITING 报名轮次晚于赛事当前轮次时为 ADVANCED，轮次相等时才为 WAITING_MATCH；关联赛约/线下活动压缩为卡片。赛约缺失失败，球场缺失则用室外硬地与开始时段背景。
6. 批量补昵称、性别、NTRP、头像，资料缺失字段留空；赛事图与头像生成 3600 秒地址。未读查询不推进已读。

## 边界情况

- 本活动在流程中访问记录前后分段执行，后段失败不会回滚访问时间。
- 对手手机号宁可整体不交付，也不在身份关系含糊时猜测。
- 详情查询本身不改变赛事、比赛或报名状态。

## 实现提示

纯查询部分按 DB snapshot 声明；签名与卡片组装失败按现有 Java 整体收敛。
