---
id: "@tour.tournament"
kind: aggregate
tables:
  - name: tour_tournament
    columns: [id, tournament_id, year, name, tour, category, surface, city, country, prize_money, prize_money_text, status, start_date, end_date, image_path, background_path, create_time, update_time]
---

## 概要

维护职业赛事年度主档及其展示图片绑定。

## 聚合清单

### 聚合根

| 名称 | 标识 | 标识生成 | 承载 | 表 |
|---|---|---|---|---|
| 职业赛事年度 | `tournament_id+year` | 来源赛事编号与年份组合，数据库生成内部 id | 名称、巡回赛、级别、场地、地点、奖金、日期、状态与图片 | `tour_tournament` |

### 实体

无

### 值对象

| 名称 | 由什么构成 | 落在哪 |
|---|---|---|
| 赛事身份 | 来源赛事编号、年份 | `tournament_id`、`year` |
| 赛事分类 | 巡回赛、级别、场地类型 | `tour`、`category`、`surface` |
| 举办信息 | 名称、城市、国家、开始日、结束日 | `name`、`city`、`country`、`start_date`、`end_date` |
| 奖金快照 | 标准金额、来源展示文本 | `prize_money`、`prize_money_text` |
| 图片绑定 | 主图资源标识、背景图资源标识 | `image_path`、`background_path` |

### 外部引用

| 名称 | 标识 | 指向 | 说明 |
|---|---|---|---|
| 图片资源 | `image_path/background_path` | `@content.tournament-image-asset` | 只保存资源键，不装载或删除对象存储内容 |

## 边界

一次加载与保存的单位是一个 `tournament_id+year` 职业赛事年度。签表、比赛和参赛球员在其他聚合；按赛事编号跨年份绑定图片时，调用方对每个匹配年度聚合分别执行相同命令。

## 状态

| 状态 | 含义 | 可迁移到 | 触发命令 |
|---|---|---|---|
| `ACTIVE` | 来源认为赛事尚未结束或仍有效 | `ACTIVE/COMPLETED` | `C1` |
| `COMPLETED` | 来源认为赛事已经结束 | `ACTIVE/COMPLETED` | `C1` |

状态是来源主档快照，允许后续来源纠正，不作为单向业务流程。

## 不变量

| 编号 | 约束 | 涉及聚合内哪些对象 | 为什么必须在一次事务内保证 | 违反时的错误标识 |
|---|---|---|---|---|
| I1 | `tournament_id+year` 唯一且建立后不可修改；tour 不参与身份 | 赛事根、赛事身份 | 跨年份数据不得互相覆盖，同时要与数据库及采集定位方式一致 | `TOUR_TOURNAMENT_IDENTITY_CONFLICT` |
| I2 | 名称、tour、场地、城市及起止日期必填，开始日不得晚于结束日；奖金若有不得为负 | 赛事根、分类、举办信息、奖金快照 | 主档字段必须作为一致快照落库，避免不可展示或时间倒置的赛事 | `TOUR_TOURNAMENT_PROFILE_INVALID` |
| I3 | 名录刷新不得修改图片；图片命令必须同时提供主图和背景图资源标识并成对替换 | 赛事根、图片绑定 | 主档采集和内容生产拥有不同更新意图，必须防止互相擦除 | `TOUR_TOURNAMENT_IMAGE_BINDING_INVALID` |

## 命令

| 编号 | 命令 | 前置状态 | 入参 | 后置状态 | 拒绝情形 |
|---|---|---|---|---|---|
| C1 | 新增或刷新赛事名录 | 同身份不存在或 `ACTIVE/COMPLETED` | tournamentId、year 与完整来源主档 | 新建或替换主档字段，保留图片绑定 | 身份缺失/冲突；必填字段缺失；日期或奖金非法 |
| C2 | 替换赛事图片绑定 | `ACTIVE/COMPLETED` | 非空主图资源键、非空背景图资源键 | 状态不变，两项图片绑定一起替换 | 任一资源键缺失；赛事不存在时由调用方空操作 |

## 边界情况

- 同一 `tournament_id` 跨年份形成多个聚合；图片维护会分别更新所有已存在年份。
- 同编号同年同时来自 ATP/WTA 时不会形成两条记录，后一次成功名录刷新覆盖 tour 等主档字段。
- 名录刷新保留 `image_path/background_path`；图片绑定不修改任何名录字段。
- 来源未出现的存量赛事不删除、不失效；空来源批次不产生命令。
- 重复绑定相同资源键幂等成功；并发绑定最终以最后成功提交者为准。
- 图片绑定找不到赛事时空操作成功，已上传对象不补偿删除。

## 实现提示

`uk_tour_tournament_tournament_year` 保护复合自然键。名录更新 SQL 明确排除图片列；图片更新则只写两个图片列。跨年份绑定先按 `tournament_id` 找出根标识，再逐根执行 `C2`，不把多年度记录合成一个聚合。
