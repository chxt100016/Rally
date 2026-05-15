# 网球约球系统 · 产品设计规格

**日期：** 2026-05-15  
**平台：** 微信小程序（主）  
**阶段：** MVP

---

## UI 设计稿（已确认）

| 页面 | 文件 | 说明 |
|------|------|------|
| 首页「发现约球」 | [ui-home-layout.html](ui-home-layout.html) | 列表流 + Tab 筛选（方案 A） |
| 发布约球弹窗 | [ui-publish-modal.html](ui-publish-modal.html) | 底部半屏弹窗，含 NTRP 滚轮、AA 费用分摊 |
| 约球详情页 | [ui-detail-layout.html](ui-detail-layout.html) | 顶部标题区 + 独立信息模块 + 下方卡片 |
| 「我的」页面 | [ui-mine-layout.html](ui-mine-layout.html) | 快捷入口 + 可信度一行 + 评价摘要 |

---

## 一、系统概览

网球约球系统，面向个人网球选手，核心价值是「快速找到合适的球友约球」。

**底部导航：**
- `发现约球`（首页）
- `+`（悬浮按钮，点击弹出发布弹窗）
- `我的`

---

## 二、用户模块

### 2.1 注册与登录

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | string | 系统唯一 ID |
| `channel` | enum | `phone` / `wechat_miniapp` |
| `phone` | string | 手机号，phone 渠道必填 |
| `wechat_openid` | string | 微信渠道必填 |
| `wechat_unionid` | string | 微信渠道，用于跨端识别 |
| `nickname` | string | 昵称 |
| `avatar_url` | string | 头像 URL |
| `created_at` | datetime | 注册时间 |

### 2.2 球员信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `profile_id` | string | 关联 user_id |
| `self_score` | float | 自评分，1.0~5.0，步长 0.5 |
| `self_video_urls` | string[] | 打球视频列表，最多 5 个 |
| `ntrp_level` | float | NTRP 自填，1.5~7.0，步长 0.5 |
| `ntrp_verified` | boolean | 是否经三方认证 |
| `utr_score` | float | UTR 评分（三方接入，选填） |
| `bio` | string | 个人简介，最多 100 字 |
| `updated_at` | datetime | 最后更新时间 |

### 2.3 他人评价（标签）

| 字段 | 类型 | 说明 |
|------|------|------|
| `review_id` | string | 评价 ID |
| `from_user_id` | string | 评价人 |
| `to_user_id` | string | 被评价人 |
| `booking_id` | string | 关联约球，同一约球只能评价一次 |
| `tags` | string[] | 标签列表，见预设标签 |
| `created_at` | datetime | 评价时间 |

**预设标签（MVP）：**
正手稳、反手稳、发球好、底线稳、网前好、移动快、守时、友善、爽约（负向）

### 2.4 可信度体系

| 字段 | 类型 | 说明 |
|------|------|------|
| `credibility_score` | int | 0~100，初始 0 |
| `credibility_level` | enum | `none(0-19)` / `junior(20-49)` / `mid(50-79)` / `senior(80-100)` |

**积分规则（MVP）：**

| 行为 | 积分 |
|------|------|
| 完成一次约球（参与方） | +5 |
| 完成一次约球（发布方） | +3 |
| 收到他人正向标签评价 | +3 / 条 |
| 上传打球视频 | +5 / 个，上限 +25 |
| 爽约（被标记） | -10 |

---

## 三、约球模块

### 3.1 约球列表页（首页）

**布局：** 列表流 + Tab 筛选

| 元素 | 说明 |
|------|------|
| 顶部位置筛选 | 显示当前城市/区域，可点击修改 |
| Tab 栏 | 全部 / 推荐 / 单打 / 双打 |
| 约球卡片 | 见下方字段 |

**约球卡片展示字段：**
- 标题（自动生成或用户填写）
- 时间（日期 + 开始时间）
- 场地名称
- 类型（单打/双打/多人）
- 当前人数 / 上限人数
- 期望水平（NTRP 范围/指定/以上/以下）
- 水平匹配标签（✓ 匹配 / ⚠ 偏高 / ⚠ 偏低）
- 报名模式（直接加入 / 需审核）

**推荐逻辑（MVP）：**
- 与当前用户 NTRP 差值 ≤ 1.0 的约球优先展示
- 同城市优先

### 3.2 发布约球

**交互：** 点击底部 `+` 按钮，底部弹出半屏弹窗

#### 表单字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | string | 否 | 选填；不填时自动生成：`{类型} · {星期} {时间} · {场地简称}` |
| `match_type` | enum | 是 | `single`（单打）/ `double`（双打）/ `multi`（多人） |
| `max_players` | int | 是 | 人数上限，最小 2，最大 10 |
| `date` | date | 是 | 约球日期 |
| `start_time` | time | 是 | 开始时间 |
| `duration` | float | 是 | 时长，滚轮选择：0.5 / 1.0 / 1.5 / 2.0 / 2.5 / 3.0（小时） |
| `venue_name` | string | 是 | 场地名称，来自地图选点 POI |
| `venue_address` | string | 是 | 详细地址 |
| `venue_lat` | float | 是 | 纬度 |
| `venue_lng` | float | 是 | 经度 |
| `level_mode` | enum | 否 | `range`（范围）/ `exact`（指定）/ `above`（以上）/ `below`（以下） |
| `level_min` | float | 否 | NTRP 下限，步长 0.5，范围 1.5~7.0 |
| `level_max` | float | 否 | NTRP 上限，步长 0.5，范围 1.5~7.0 |
| `join_mode` | enum | 是 | `direct`（直接加入）/ `approval`（需审核） |
| `min_credibility` | enum | 是 | `none` / `junior` / `mid` / `senior`，默认 `none` |
| `cost_items` | array | 是 | 费用项列表，见下方结构 |

**cost_items 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 费用项名称，如「球场费」「球费」 |
| `total_amount` | int | 总费用（分），前端展示元 |

**费用分摊计算：**
`每人分摊 = sum(cost_items.total_amount) ÷ max_players`，人数变化时实时重算，满员后锁定。

**NTRP 选择器交互：**
- 平时显示文本框（如 `3.5 ~ 4.5`）
- 点击数值框触发底部滚轮选择器
- 滚轮可选值：1.5 / 2.0 / 2.5 / 3.0 / 3.5 / 4.0 / 4.5 / 5.0 / 5.5 / 6.0 / 6.5 / 7.0

### 3.3 约球详情页

**页面结构（从上到下）：**

1. **顶部标题区**（蓝色背景）
   - 标题
   - 状态标签：水平匹配状态 / 报名模式 / 招募状态

2. **信息模块**（白色卡片）
   - 2×2 网格：类型 / 人数 / 时间（含时长）/ 水平要求
   - 场地单独一行：场地名 + 详细地址 + 「查看地图 ›」

3. **发布者卡片**
   - 头像 / 昵称 / NTRP / 可信度星级
   - 「查看主页 ›」入口

4. **参与者卡片**
   - 头像列表（已加入人员）
   - 空位用虚线圆圈占位

5. **AA 费用卡片**
   - 各费用项明细
   - 每人分摊 = 总费用 ÷ 人数

6. **底部操作按钮**
   - 招募中 + 未报名：「立即报名」/ 「申请加入」（按报名模式）
   - 已报名：「已报名」（不可点击）
   - 已满员：「已满员」（不可点击）
   - 发布者视角：「管理约球」

### 3.4 约球数据模型

| 字段 | 类型 | 说明 |
|------|------|------|
| `booking_id` | string | 唯一 ID |
| `creator_id` | string | 发布者 user_id |
| `title` | string | 标题（自动生成或用户填写） |
| `match_type` | enum | `single` / `double` / `multi` |
| `max_players` | int | 人数上限 |
| `current_players` | int | 当前已加入人数 |
| `date` | date | 约球日期 |
| `start_time` | time | 开始时间 |
| `duration` | float | 时长（小时） |
| `venue_name` | string | 场地名称 |
| `venue_address` | string | 详细地址 |
| `venue_lat` | float | 纬度 |
| `venue_lng` | float | 经度 |
| `level_mode` | enum | `range` / `exact` / `above` / `below` |
| `level_min` | float | NTRP 下限（nullable） |
| `level_max` | float | NTRP 上限（nullable） |
| `join_mode` | enum | `direct` / `approval` |
| `min_credibility` | enum | `none` / `junior` / `mid` / `senior` |
| `cost_items` | json | 费用项数组 |
| `status` | enum | `open`（招募中）/ `full`（已满）/ `closed`（已关闭）/ `finished`（已结束） |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

### 3.5 报名记录

| 字段 | 类型 | 说明 |
|------|------|------|
| `application_id` | string | 唯一 ID |
| `booking_id` | string | 关联约球 |
| `user_id` | string | 报名用户 |
| `status` | enum | `pending`（待审核）/ `approved`（已通过）/ `rejected`（已拒绝） |
| `created_at` | datetime | 报名时间 |

---

## 四、「我的」页面

**页面结构（从上到下）：**

1. **顶部个人信息**（蓝色背景）
   - 头像 / 昵称
   - NTRP 等级 + 可信度数值（一行摘要）
   - 「编辑资料」按钮

2. **快捷入口**（4 格）
   - 我发布的 / 我参与的 / 我的评价 / 我的视频

3. **可信度**（一行）
   - 进度条 + 数值 + 「详情 ›」入口

4. **球员评价摘要**
   - 自评分 / NTRP / 他评次数（3 格）
   - 他人标签 Top 展示

---

## 五、功能边界（MVP 范围）

**包含：**
- 手机号注册 + 微信小程序注册
- 球员信息完善（自评分、NTRP、视频上传）
- 他人标签评价
- 可信度积分体系
- 约球发布（底部弹窗）
- 约球列表（全部 + 推荐 Tab）
- 约球详情 + 报名
- 「我的」页面

**不包含（后续迭代）：**
- UTR 三方接入
- 场地数据库
- 消息通知系统
- 约球内聊天
- 地图找球场功能
