# 模块 3：匹配（定时批量匹配）

## 职责
每天凌晨 2:00 批量匹配赛事当前轮次。资格赛按 qualifierGroupSize 分组，正赛固定两队一场。匹配算法作为**领域能力**实现，不写在 SQL 里。产出新的 TournamentMatch 及其参与者，并把候选人从 WAITING 推进为 IN_MATCH。

## 聚合根 / 领域对象
- **TournamentEntry / entryNo 队伍**：匹配的输入（WAITING + 当前轮次的候选人）。单打一个 entryNo 对应一人，双打同 entryNo 的两人是一支队伍；匹配后其 status→IN_MATCH。
- **TournamentMatch（聚合根）+ MatchParticipant**：匹配的产出。
  - Match：tournamentId、matchNo（赛事内递增，展示补零3位）、round、groupSize、status（初始 MATCHED 或 BOOKING）、matchedTime、version（乐观锁）。
  - Participant：每个参与者一条，含 teamId（双打同队相同、单打为空）、confirmStatus、resultConfirmStatus。

## 领域 Service 能力

### TournamentMatchingService（纯算法，无副作用）
输入：当前轮次的 entryNo 队伍 + 本轮组大小 + 本轮已完成比赛的对阵历史；输出：`List<MatchGroup>`。
1. **活动区域与可打时间交集**：队伍的偏好区域取成员并集；双打队伍的可打时间取成员交集。任意两队无地区交集不可分到一组，且整场所有队伍必须存在共同可打时间。
2. **全局最优分组**：枚举可行分组并优先最大化成功匹配队伍数，不使用局部贪心。
3. **订场能力优先**：优先恰好一名 `CAN_BOOK` 成员的分组（可直接指定订场人），再优先至少一名 `CAN_BOOK` 成员的分组；无人可订场的分组仍允许作为兜底。
4. **性别优先**：在不减少成功匹配队伍数的前提下，优先相同性别构成的队伍对阵；双打比较整队构成（男双、女双、混双）。
5. **重复对阵**：同轮已完成比赛的 entryNo 组合不重复安排；仅最后恰好剩下一组时允许兜底。
6. **报名先后**：其他条件相同时，优先更早报名的队伍。

### TournamentMatchAssembleService（落地产出）
- 对每个 MatchGroup：分配 matchNo（当前赛事 max+1，Redis 计数器/`FOR UPDATE` 保证并发安全）、创建 Match + Participant、置候选人 IN_MATCH。
- **初始状态判定**：恰好一人 CAN_BOOK 其余 CANNOT_BOOK → 直接 BOOKING，该人为订场人；否则（都能/都不能订场）→ MATCHED，等先到先得。
- 转线下轮次（round ≥ offlineFromRound 对应轮）：平台负责场地，跳过订场流程，直接进入待比赛/由运营安排（与模块 4 状态机约定）。

### TournamentBatchMatchService（批量匹配领域能力）
- 查询已到资格赛开始时间的激活赛事。
- 对每个赛事只执行 `currentRound` 的匹配，返回本次新产出的 `TournamentMatch`。
- 只负责领域规则和比赛落地，不依赖 Job、HTTP Controller 或通知模板。

## 调用入口

### 每日凌晨 2 点批量匹配 Job
位于 `rally-adapter/com.rally.job`，开关 `job.tournamentMatch.enabled`，cron 在 `application-prod.yml`。Job 只调用 `TournamentAdminAppService.runTournamentMatch()`；开关关闭时不创建 Job Bean。

### 运营后台手动匹配
`POST /tournament/admin/match/run`，直接调用同一个 `TournamentAdminAppService.runTournamentMatch()`，不依赖定时 Job 是否开启。

### 应用编排逻辑
`TournamentAdminAppService` 负责：
1. 扫描所有 ACTIVE 且已过 qualifierStartTime 的赛事。
2. 仅按赛事 `currentRound` 取 `status=WAITING` 的 Entry，并按 entryNo 聚合成队伍；资格赛使用 qualifierGroupSize，正赛使用 2。
3. 调 MatchingService 全局分组，Assemble 落地。
4. 对本次新产出的比赛触发 `TOURNAMENT_MATCHED` 通知。
5. 幂等：已进入 IN_MATCH 的 Entry 不再参与后续匹配。

## 与其他模块的边界
- 候选人来源于模块 2/4/5（报名成功、比赛被拒回池、支付进正赛）。
- 产出的 Match 交给模块 4 走状态机。
- 只查候选、只产出分组，业务规则在领域层，Repository 仅做查询与持久化。
