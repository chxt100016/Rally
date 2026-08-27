---
id: "@tour.draw"
kind: aggregate
tables:
  - name: tour_draw
    columns: [id, tournament_id, year, draw_type, size, total_rounds, create_time, update_time]
---

## 概要

保存职业赛事年份项目签表的原始身份与可选结构字段。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 巡回赛签表 | `tournament_id+year+draw_type` | 来源自然键，数据库生成内部 id | 项目类型、签位规模与总轮数 | `tour_draw` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 签表身份 | 来源赛事编号、年份、项目类型 | `tournament_id`、`year`、`draw_type` |
| 签表结构 | 签位规模、总轮数 | `size`、`total_rounds` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 来源赛事 | `tournament_id+year` | `@tour.tournament` | 只保存来源身份，不装载赛事 |

## 边界

一次加载与保存的单位是一个赛事年份下的一种项目签表。比赛和报名属于其他聚合，失败不回滚签表身份。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `PLACEHOLDER` | 身份已知，两个结构字段都未知 | `PARTIAL`、`STRUCTURED` | `C2` |
| `PARTIAL` | size 与 totalRounds 仅一项已知，或保留来源的零值 | `PARTIAL`、`STRUCTURED` | `C2` |
| `STRUCTURED` | 两项均有来源值，但不要求数学一致 | `PARTIAL`、`STRUCTURED` | `C2` |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 赛事编号、正数年份、项目类型组成唯一身份，建立后不可修改 | 签表根、签表身份 | 身份漂移会把已采集比赛挂到另一赛事项目 | `TOUR_DRAW_IDENTITY_CONFLICT` |
| I2 | drawType 使用来源原始非空代码参与唯一身份，不做别名归一；ATP/WTA 路径可分别保存 `MS/MD/LS/LD` | 签表身份 | 比赛快照和查询同样使用来源代码，改写会产生不一致或重复身份 | 空类型按保存失败传播 |
| I3 | size 与 totalRounds 是两个独立可空字段；更新时各自仅在新值非 null 时覆盖。允许 size=0，不校验 2 次幂，也不要求 totalRounds 与 size 的对数关系 | 签表结构 | 保留各来源不完整、零值或分阶段补充的 main 数据 | 无专用拒绝 |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 关联或建立签表 | 自然键不存在或已存在 | 已收录赛事、年份、来源原始 drawType、可选 size/totalRounds | 新建并保存当前可用字段；已存在返回内部 id 并进入 C2 非空刷新 | 赛事未收录或来源不可识别；数据库保存失败 |
| C2 | 刷新签表结构 | `PLACEHOLDER/PARTIAL/STRUCTURED` | 可选 size 与 totalRounds | 两字段分别非空覆盖，null 保留存量；两项都 null 时只保持身份 | 试图修改自然身份；数据库更新失败 |

## 边界情况

- 实时来源只有身份：允许建立 PLACEHOLDER，不臆测规模。
- 两个结构字段都空：只确保身份存在；只给其中一项时只刷新该项且不清另一项。
- 重复或并发采集自然键：唯一键收敛后返回同一内部 id。
- 来源遗漏既有签表：不删除、不失效。
- 双打开关关闭：调用方不发命令；聚合仍支持合法双打类型。
- 后续比赛保存失败：签表保留供下次补采。
- `LS/LD` 与 `WS/WD` 不互相归一，若来源分别提供则按不同自然身份保存。
- size=0、非 2 次幂或与 totalRounds 不一致时仍按来源保存。

## 实现提示

`uk_tour_draw_tournament_year_type` 保护原始 `(tournamentId,year,drawType)` 自然键。仓储按自然键查后插入或更新，size/totalRounds 使用各自非 null 覆盖；不得把 `LS/LD` 改成 `WS/WD`，也不得追加数学结构校验。
