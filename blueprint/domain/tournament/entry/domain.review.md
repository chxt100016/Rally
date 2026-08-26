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
