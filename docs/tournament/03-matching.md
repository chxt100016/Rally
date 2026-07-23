# 模块 3：匹配（定时批量匹配）

## 职责
每天凌晨 2:00 批量匹配。资格赛按 qualifierGroupSize 分组，正赛逐轮随机两两匹配。匹配算法作为**领域能力**实现，不写在 SQL 里。产出新的 TournamentMatch 及其参与者，并把候选人从 WAITING 推进为 IN_MATCH。

## 聚合根 / 领域对象
- **TournamentEntry**：匹配的输入（WAITING + 当前轮次的候选人），匹配后其 status→IN_MATCH。
- **TournamentMatch（聚合根）+ MatchParticipant**：匹配的产出。
  - Match：tournamentId、matchNo（赛事内递增，展示补零3位）、round、groupSize、status（初始 MATCHED 或 BOOKING）、matchedTime、version（乐观锁）。
  - Participant：每个参与者一条，含 teamId（双打同队相同、单打为空）、confirmStatus、resultConfirmStatus。

## 领域 Service 能力

### TournamentMatchingService（纯算法，无副作用）
输入：候选人列表 + qualifierGroupSize + 拒绝历史；输出：`List<MatchGroup>`。
1. **活动区域交集**：代码里做 Set 交集，无交集不可分到一组。
2. **排除互相拒绝过的组合**：优先避让曾互相拒绝者，除非无法凑齐（兜底强制，如最后剩 2 人）。
3. **随机分组**：Collections.shuffle 后按 groupSize 分批。
4. **落单处理**：凑不齐一组的人放回候选池，等下次匹配。

### TournamentMatchAssembleService（落地产出）
- 对每个 MatchGroup：分配 matchNo（当前赛事 max+1，Redis 计数器/`FOR UPDATE` 保证并发安全）、创建 Match + Participant、置候选人 IN_MATCH。
- **初始状态判定**：恰好一人 CAN_BOOK 其余 CANNOT_BOOK → 直接 BOOKING，该人为订场人；否则（都能/都不能订场）→ MATCHED，等先到先得。
- 转线下轮次（round ≥ offlineFromRound 对应轮）：平台负责场地，跳过订场流程，直接进入待比赛/由运营安排（与模块 4 状态机约定）。

## 接口清单（内部，无对外 HTTP）

### 每日凌晨 2 点批量匹配 Job
位于 `rally-adapter/com.rally.job`，开关 `job.tournamentMatch.enabled`，cron 在 `application-prod.yml`。逻辑：
1. 扫描所有 ACTIVE 且已过 qualifierStartTime 的赛事。
2. 资格赛：取该赛事 stage=QUALIFY & status=WAITING 的 Entry，若席位已满则跳过资格赛匹配。调 MatchingService 分组，Assemble 落地。
3. 正赛：按当前轮次取 stage=MAIN & status=WAITING 的 Entry，逐轮两两匹配（groupSize=2），同样避让互相拒绝组合。
4. 幂等：同一 Entry 一次 Job 内只进一组；已 IN_MATCH 不参与。

## 与其他模块的边界
- 候选人来源于模块 2/4/5（报名成功、比赛被拒回池、支付进正赛）。
- 产出的 Match 交给模块 4 走状态机。
- 只查候选、只产出分组，业务规则在领域层，Repository 仅做查询与持久化。
