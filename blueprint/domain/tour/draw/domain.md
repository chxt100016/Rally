---
id: "@tour.draw"
kind: aggregate
tables:
  - name: tour_draw
    columns: [id, tournament_id, year, draw_type, size, total_rounds, create_time, update_time]
---

## 概要

守护职业赛事一个年份项目签表的唯一身份与结构一致。

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
| `PLACEHOLDER` | 身份已知，两个结构字段都未知 | `STRUCTURED` | `C2` |
| `STRUCTURED` | 规模与轮数均已知且一致 | `STRUCTURED` | `C2` |

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | 赛事编号、正数年份、项目类型组成唯一身份，建立后不可修改 | 签表根、签表身份 | 身份漂移会把已采集比赛挂到另一赛事项目 | `TOUR_DRAW_IDENTITY_CONFLICT` |
| I2 | 类型只接受 `MS/WS/MD/WD/XD`，来源别名进入前规范化 | 签表身份 | 多种代码表达同一项目会绕过唯一键形成重复签表 | `TOUR_DRAW_TYPE_INVALID` |
| I3 | 占位时结构字段都空；结构化时 size 为正的 2 次幂且 `totalRounds=log2(size)`，两项同时写 | 签表结构 | 半结构无法确定轮次边界，必须同改同回滚 | `TOUR_DRAW_STRUCTURE_INVALID` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 关联或建立签表 | 自然键不存在或已存在 | 已收录赛事、年份、规范项目类型 | 新建 `PLACEHOLDER`；已存在幂等返回内部 id | 赛事未收录；年份/类型非法 |
| C2 | 刷新签表结构 | `PLACEHOLDER/STRUCTURED` | 非空 size 与 totalRounds | `STRUCTURED`，整体替换结构 | 只给一项；违反 I3；试图改身份 |

## 边界情况

- 实时来源只有身份：允许建立 PLACEHOLDER，不臆测规模。
- 两个结构字段都空：不刷新；只给其中一项：拒绝且不清存量。
- 重复或并发采集自然键：唯一键收敛后返回同一内部 id。
- 来源遗漏既有签表：不删除、不失效。
- 双打开关关闭：调用方不发命令；聚合仍支持合法双打类型。
- 后续比赛保存失败：签表保留供下次补采。

## 实现提示

`uk_tour_draw_tournament_year_type` 保护自然键。来源代码先映射为领域类型，结构更新同时写两列，禁止忽略 null 形成半结构。
