# 模块 2：报名（用户端）

## 职责
用户在报名池开放期间报名赛事、修改报名偏好、退出赛事。管理 TournamentEntry 从无到 WAITING（资格赛排队）的诞生，以及主动退出（WITHDRAWN）。

赛事仅通过 banner 暴露入口，无列表页，用户从 banner 直达落地页后在此报名。

## 聚合根 / 领域对象
- **TournamentEntry（聚合根）**：一个用户在一个赛事内唯一一条报名（`uk_tournament_user`）。
  - 关键字段：tournamentId、userId、partnerId（双打搭档）、preferredDistricts（区域 JSON）、courtAbility（CAN_BOOK/CANNOT_BOOK）、availableTimes（JSON）、stage（QUALIFY/MAIN）、status（WAITING/IN_MATCH/PAYING/ELIMINATED/WITHDRAWN）、currentRound、拒绝计数、qualifiedTime、paidTime。
  - 诞生：报名成功即 stage=QUALIFY、status=WAITING、currentRound=QUALIFIER。
  - 昵称/NTRP/性别不冗余，用 userId 回查用户域。

## 领域 Service 能力（TournamentEntryService）
- `join(tournamentId, userId, 偏好)`：校验赛事 ACTIVE、在报名开放窗口内、性别/NTRP 符合限制、未重复报名；创建 Entry（WAITING）。
- `updatePreference(entry, 偏好)`：报名信息可随时修改（区域/场地能力/可比赛时间），仅在排队阶段（未进入 IN_MATCH）允许，避免比赛中改动影响匹配。
- `withdraw(entry)`：退出。资格赛阶段直接置 WITHDRAWN；正赛阶段（已支付）需先触发退款（委托模块 5），退款成功后释放席位再置 WITHDRAWN。
- 获取领域对象用 EntryService，不直接调 gateway。

## 接口清单

### `POST /tournament/entry/join`
报名。入参 `TournamentJoinCmd`（tournamentId、可选 partnerId、preferredDistricts、courtAbility、availableTimes）。userId 从 `UserContext.get()` 取。校验：赛事状态 ACTIVE、当前时间 ≥ registrationStartTime 且（若设了 registrationEndTime）≤ 截止、性别限制、是否已报名（唯一约束兜底）。成功返回 entry 概要 DTO。

### `POST /tournament/entry/update`
修改报名偏好。入参 `TournamentEntryUpdateCmd`（entryId/tournamentId + 偏好字段）。仅本人、仅排队态可改。用于影响下一次凌晨匹配。

### `POST /tournament/entry/withdraw`
退出赛事。入参含 tournamentId。领域层按 stage 分流：
- QUALIFY（WAITING/IN_MATCH）：无费用，直接 WITHDRAWN。若正在 IN_MATCH，需处理其对应比赛（对手判胜/回池，具体规则与模块 4 协同）。
- MAIN（已支付）：调模块 5 退款流程，成功后 currentFilledSlots-1，再置 WITHDRAWN。
返回退出结果 DTO（是否触发退款）。

## 与其他模块的边界
- 报名产生的 WAITING Entry 是模块 3 匹配的候选来源。
- withdraw 的退款与席位释放逻辑属模块 5，本模块编排调用。
- 修改偏好只影响匹配输入，不触碰进行中的 Match。
