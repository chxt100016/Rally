# 首页接口文档

**Base URL**: `/api/rally/home`

---

## 1. 获取首页聚合数据

**GET** `/page`

聚合首页多种展示模块（约球、赛事、海报、资讯），后端已完成排序，前端按 `displayItems` 数组顺序渲染即可。

**鉴权**：可选登录（未登录也可访问，登录态用于个性化数据，如"我的进行中约球"）。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `cityCode` | `string` | 否 | 城市编码。不传时，返回结果中不会包含海报卡片（`POSTER_CARD`）模块 |

**示例**
```
GET /api/rally/home/page?cityCode=310100
```

---

## 通用响应结构

```json
{
  "code": 0,
  "message": null,
  "data": {
    "displayItems": [
      { "displayType": "POSTER_CARD", "data": { "...": "..." } },
      { "displayType": "MEETUP", "data": { "...": "..." } },
      { "displayType": "TOUR_MATCH", "data": { "...": "..." } },
      { "displayType": "NEWS_TIMELINE", "data": { "...": "..." } }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `number` | `0` 表示成功，非 0 表示业务错误 |
| `message` | `string\|null` | 错误信息，成功时为 null |
| `data.displayItems` | `HomeDisplayItemDTO[]` | 首页展示项列表，**顺序即展示顺序**，前端不应自行重排 |

**重要**：数组中每一项都可能缺失（不保证一定出现），前端应按 `displayType` 分发渲染，未识别的 `displayType` 直接忽略跳过（便于后端后续扩展新类型不影响老版本前端）。

---

## 数据结构

### `HomeDisplayItemDTO` — 首页展示项

| 字段 | 类型 | 说明 |
|---|---|---|
| `displayType` | `DisplayType` | 展示类型枚举，决定 `data` 的具体结构，见下方枚举说明 |
| `data` | 对象 | 展示数据，具体结构由 `displayType` 决定，见下方各类型说明 |

### `DisplayType` — 展示类型枚举

| 枚举值 | 说明 | 对应 `data` 结构 |
|---|---|---|
| `MEETUP` | 约球活动 | `MeetupDisplayData` |
| `TOUR_MATCH` | 赛事 | `MatchDisplayData` |
| `POSTER_CARD` | 海报卡片 | `PosterCardDisplayData` |
| `NEWS_TIMELINE` | 资讯时间线 | `NewsTimelineDisplayData` |

---

### 1. `MeetupDisplayData` — 约球活动模块

**出现条件**：始终出现。

| 字段 | 类型 | 说明 |
|---|---|---|
| `title` | `string` | 固定为"我的进行中约球" |
| `subtitle` | `string\|null` | 无，恒为 null |
| `meetups` | `MeetupCardDTO[]` | 约球卡片列表。**未登录时为空数组**；登录时返回当前用户"进行中"tab 的约球列表 |

#### `MeetupCardDTO` — 约球卡片

| 字段 | 类型 | 说明 |
|---|---|---|
| `meetupId` | `string` | 约球 ID |
| `title` | `string` | 约球标题 |
| `matchType` | `MatchTypeEnum` | 约球类型：`SINGLE`单打 / `DOUBLE`双打 / `RALLY`拉球 |
| `maxPlayers` | `number` | 最大人数 |
| `currentPlayers` | `number` | 当前已报名人数 |
| `startTime` | `string` | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `duration` | `number` | 时长（小时） |
| `cityName` | `string` | 城市名 |
| `districtName` | `string` | 区域名 |
| `courtName` | `string` | 球场名 |
| `levelMode` | `LevelModeEnum` | 水平要求模式：`RANGE`区间 / `EXACT`精确 / `ABOVE`高于 / `BELOW`低于 |
| `levelMin` | `string` | 水平下限 |
| `levelMax` | `string` | 水平上限 |
| `status` | `MeetupStatusEnum` | 约球状态：`OPEN`报名中 / `ONGOING`进行中 / `CLOSED`关闭 / `FINISHED`已结束 |
| `distanceKm` | `number\|null` | 距离当前用户的公里数，无定位信息时为 null |
| `primaryLabel` | `string` | 主标签文案：`OPEN` 状态时展示区域名，其余状态展示状态文案，直接展示即可无需前端二次翻译 |

---

### 2. `MatchDisplayData` — 赛事模块

**出现条件**：仅当存在有效的巡回赛赛事数据时才出现；无数据时整个 `TOUR_MATCH` 展示项不会出现在 `displayItems` 中（前端无需处理"空赛事卡片"的兜底展示）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `title` | `string` | 固定为"巡回赛进行中" |
| `subtitle` | `string` | 格式 `x场ATP、x场WTA`，x 为当前有效赛事按巡回赛类型（`tour`）统计的数量 |
| `tournaments` | `TournamentDisplayDTO[]` | 赛事列表，每个元素代表一场赛事 |

#### `TournamentDisplayDTO` — 赛事展示项

每个赛事只展示其"最近一个未开始日期"的"第一个球场"的比赛列表，剩余信息需点击"查看更多"跳转赛事详情页查看。

| 字段 | 类型 | 说明 |
|---|---|---|
| `tournamentId` | `string` | 赛事 ID，用于拼接跳转赛事详情页链接 |
| `tournamentName` | `string` | 赛事名称 |
| `courtName` | `string` | 球场名称（该赛事下第一个球场） |
| `matchDate` | `string` | 比赛日期，格式 `yyyy-MM-dd` |
| `matches` | `MatchBriefDTO[]` | 该球场当天的比赛简要列表 |

#### `MatchBriefDTO` — 比赛简要信息

| 字段 | 类型 | 说明 |
|---|---|---|
| `matchId` | `string` | 比赛 ID |
| `roundName` | `string` | 轮次展示文案（如"1/4决赛"），直接展示 |
| `player1Name` | `string\|null` | 球员1姓名，轮空/未定时可能为 null |
| `player2Name` | `string\|null` | 球员2姓名，轮空/未定时可能为 null |
| `status` | `string` | 比赛状态展示文案，直接展示 |

---

### 3. `PosterCardDisplayData` — 海报卡片模块

**出现条件**：仅当请求携带 `cityCode` 时才出现；未传 `cityCode` 时不出现。

| 字段 | 类型 | 说明 |
|---|---|---|
| `title` | `string` | 固定为"球场" |
| `subtitle` | `string` | 固定为"寻找当前城市的球场" |
| `posters` | `PosterCardItem[]` | 海报列表，一个模块下可含多张海报图 |

#### `PosterCardItem` — 单张海报

| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | `PosterType` | 交互类型：`NAVIGATE` 点击跳转页面 / `PREVIEW` 点击放大预览图片，前端需据此区分点击行为 |
| `imageUrl` | `string` | 海报图片地址（已签名，可直接使用，有时效性，不要缓存持久化该 URL） |
| `title` | `string` | 海报标题 |
| `subtitle` | `string` | 海报副标题 |
| `wechatUrl` | `string\|null` | 微信小程序端跳转路径，已自动拼接 `?cityCode={cityCode}`；`type=PREVIEW` 时可能为 null |
| `appUrl` | `string\|null` | App 端跳转路径，已自动拼接 `?cityCode={cityCode}`；`type=PREVIEW` 时可能为 null |
| `webUrl` | `string\|null` | Web 端跳转路径，已自动拼接 `?cityCode={cityCode}`；`type=PREVIEW` 时可能为 null |

---

### 4. `NewsTimelineDisplayData` — 资讯时间线模块

**出现条件**：始终出现（当前 `newsItems` 恒为空数组，资讯数据源尚未接入，字段结构已定义供前端提前适配）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `title` | `string` | 固定为"资讯" |
| `subtitle` | `string` | 固定为"最新动态" |
| `newsItems` | `NewsItem[]` | 资讯列表，当前恒为空数组 |

#### `NewsItem` — 资讯条目

| 字段 | 类型 | 说明 |
|---|---|---|
| `newsId` | `string` | 资讯 ID |
| `title` | `string` | 标题 |
| `summary` | `string` | 摘要 |
| `publishTime` | `string` | 发布时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `coverImage` | `string` | 封面图地址 |
| `linkUrl` | `string` | 详情跳转链接 |

---

## 前端接入建议

1. 遍历 `data.displayItems`，按 `displayType` 分发到对应渲染组件；未识别类型直接跳过。
2. `MEETUP`：未登录态下 `meetups` 为空数组，可整体隐藏该模块或展示"登录查看我的约球"引导（后端不做该判断，由前端决定 UI 策略）。
3. `TOUR_MATCH`：数组中可能不存在该项，无需为"无赛事"设计空状态。
4. `POSTER_CARD`：仅在传了 `cityCode` 时请求才会返回，建议前端在拿到定位/城市信息后再请求本接口，或不传 `cityCode` 先渲染其余模块，拿到城市后二次请求刷新。
5. `imageUrl` 为签名 URL，有过期时间，请求到后直接使用，不要长期缓存。
