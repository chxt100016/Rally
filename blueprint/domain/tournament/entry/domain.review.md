# @tournament.entry 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 报名聚合按单个用户还是按双打整队划分？
  > 按赛事中的单个用户报名划分，以 bizId 为根标识、(tournamentId,userId) 为唯一自然键；搭档报名是另一聚合。
  → 支付、拒绝计数、访问时间和状态均可按个人变化；双打共享编号由注册活动跨两个根协调。

- [Q2] 哪些状态属于可回到匹配池，哪些是终态？
  > WAITING/FROZEN/IN_MATCH/PAYING 为进行态；比赛拒绝或取消只将 IN_MATCH 释放为 WAITING；ELIMINATED 与 WITHDRAWN 是终态。
  → 冻结、解冻、匹配、拒赛、退赛和取消活动共同给出了这些精确迁移。

- [Q3] 资格赛胜负和支付成功如何推进报名？
  > 资格赛胜方 IN_MATCH→PAYING 并记录 qualifiedTime，负方回 WAITING；首次有效支付使 PAYING→MAIN/WAITING/正赛首轮并记录 paidTime。
  → 赛果确认与四条支付推进路径均使用同一报名迁移语义。

- [Q4] 偏好、拒绝次数和访问时间分别采用什么更新语义？
  > 三组偏好必须整组替换；拒绝次数按当前赛段在限额校验后递增；lastVisitTime 只记录更晚访问时刻。
  → 三类字段来自互不相同的明确命令，不能合并成通用字段补丁。

- [Q5] ROUND_64 与 qualifiedTime 应如何按现有事实建模？
  > currentRound 必须纳入 ROUND_64；资格赛胜方只转 PAYING，现有活动不写 qualifiedTime，因此该列作为未启用事实保留，不能由领域臆造时间。
  → TournamentRoundEnum 与支付首轮映射支持 64 签；TournamentEntry.advanceAfterWin 只改状态，未设置 qualifiedTime。

- [Q6] lastVisitTime 是否按现实现状直接写入本次时间，不比较旧值，因此不承诺“只保留更晚时刻”？
  > 是。当前仓储直接写本次 lastVisitTime，不读取或比较旧值；文档不再承诺单调递增。
  → 已落实到不变量 I6、命令 C10 与边界情况。

- [Q7] 比赛完成结算是否继续由活动直接推进胜负报名，不在 TournamentEntry.advanceAfterWin 内额外要求原状态必须 IN_MATCH？
  > 是。比赛活动负责确认完成事实；报名胜负推进方法继续不复核原状态必须为 IN_MATCH，并按赛段与比赛轮次覆盖结果。
  → 已落实到命令 C6、比赛结算边界情况与实现提示。

- [Q8] 报名仓储是否继续按 bizId 普通更新/插入且状态保存不带原状态条件；仅数据库 uk_tournament_user 防重复用户报名，不声称所有状态命令都有乐观锁？
  > 是。只声明 uk_tournament_user 与 uk_biz_id 的真实唯一约束；普通保存无原状态或版本条件，不把匹配释放的内存过滤泛化为全部命令的乐观锁。
  → 已落实到边界情况与实现提示。

- [Q9] 匹配偏好集合中的空白、重复或其他原始元素是否应由领域新增拒绝或清洗？
  > 否。保持 main：只校验地区和时间集合非空及 courtAbility；列表元素不清洗、不去重、不做额外格式校验。
  → I3、C2 与边界情况已明确列表元素原样保留。
