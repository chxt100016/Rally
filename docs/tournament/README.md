# Tournament OS 模块索引

源方案见 [00-source-mvp.md](00-source-mvp.md)。本目录按**操作流程**将赛事系统拆分为 6 个模块，每个模块一个文件夹，内含：该模块负责的接口清单、涉及的聚合根、需要的领域 Service 能力、以及每个接口内部应做的事情。

拆分粒度：以接口为单位归属模块。设计文档只描述逻辑与职责，供后续 AI coding 分模块实现。

## 模块总览

| 模块 | 目录 | 核心操作 | 接口 |
|------|------|---------|------|
| 1. 赛事管理 | [01-admin](01-admin.md) | 运营创建/编辑/激活/废弃赛事 | 5 个后台接口 |
| 2. 报名 | [02-entry](02-entry.md) | 用户报名、修改偏好、退出 | join / update / withdraw |
| 3. 匹配 | [03-matching](03-matching.md) | 凌晨批量匹配（资格赛+正赛） | 定时 Job（内部） |
| 4. 比赛流程 | [04-match-flow](04-match-flow.md) | 订场人、订场、赛约确认、结果确认 | court-booker / book / schedule-confirm / submit-result / result-confirm |
| 5. 支付与晋级 | [05-payment](05-payment.md) | 支付锁席位、资格→正赛、退款 | entry/pay + 支付回调 + 席位管理 |
| 6. 落地页详情 | [06-detail](06-detail.md) | 聚合详情、actionState、时间线、信用 | detail |

## 聚合根全景

- **Tournament（赛事聚合根）**：赛事配置 + 席位进度 + 状态（DRAFT/ACTIVE/ABANDONED）。归属模块 1、5。
- **TournamentEntry（报名聚合根）**：一个用户在一个赛事内的报名，含 stage/status、当前轮次、拒绝计数。归属模块 2、3、5。
- **TournamentMatch（比赛聚合根）**：一场比赛 + 其参与者（MatchParticipant 为聚合内实体），承载完整状态机。归属模块 3、4、6。

## 跨模块共享枚举

集中定义，各模块引用：赛事状态、Entry 的 stage/status、Match 的 round/status、confirm/result 状态、拒绝阶段与理由编码、actionState。详见各模块内引用说明与 [00-source-mvp.md](00-source-mvp.md)。
