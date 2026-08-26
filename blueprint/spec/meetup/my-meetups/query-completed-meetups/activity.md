---
id: meetup.my-meetups.activity.query-completed-meetups
depends_on: []
reads:
  - name: rally_meetup
    columns: [biz_id, title, match_type, max_players, current_players, city_name, district_name, start_time, end_time, duration, court_name, court_id, level_mode, level_min, level_max, status]
  - name: rally_meetup_registration
    columns: [rally_meetup_id, user_id, status]
---

## 概要

查询本人报名已评价或已跳过的非草稿约球。

## 时序图

```mermaid
sequenceDiagram
    participant F as 我的约球编排
    participant A as query-completed-meetups 活动
    participant DB as 约球与报名表
    F->>A: userId、lastId 与 limit
    A->>DB: 查询 REVIEWED/SKIPPED 报名关联约球
    DB-->>A: 非草稿编号倒序窗口
    A-->>F: 候选页
```

## 触发条件

已登录用户选择 `COMPLETED` 标签后执行。

## 活动契约

入参为当前 `userId`、可选 `lastId` 和 `size+1` 的 `limit`；返回本人完成态报名关联的非草稿约球窗口。

## 异常分支

| 错误标识 | 触发条件 | 处理 |
|---|---|---|
| `SYSTEM_ERROR` | 约球或报名查询、枚举映射失败 | 终止只读活动 |

## 领域依赖

无

## 业务动作

A1 定位本人 REVIEWED 或 SKIPPED 报名
A2 排除草稿约球并应用编号游标
A3 返回编号倒序分页窗口

## 详细流程

1. 子查询要求 `user_id=userId` 且报名状态为 `REVIEWED/SKIPPED`，再选择其关联约球。
2. 约球只要求 `status!='DRAFT'`；不检查结束时间、存储 FINISHED 或创建者关系，因此错误提前进入完成态的报名也会入选。
3. 多条完成态报名关联同一约球时 `IN` 语义仍只返回一条约球。
4. 有游标时保留 `biz_id<lastId`，按编号倒序限制 `size+1`，不统计 total。

## 边界情况

- 已结束但报名仍为 JOINED 的约球不入选。
- REVIEWED/SKIPPED 约球即使存储 OPEN 且未来开始也会入选。
- 发布者若没有完成态报名，不会仅凭创建关系入选。
- 翻页期间评价状态变化可能移动记录集合。

## 实现提示

只读字段已按当前 DB snapshot 声明；“已完成”当前代表评价流程完成，而非约球时间或状态完成。
