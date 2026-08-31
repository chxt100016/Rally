# tournament.result-confirm-admin.activity.confirm-result-by-admin 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 按 tournamentId+matchNo 定位比赛聚合，是否复用赛约代确认活动同款的自然键锁定读取方式？
  > 复用：按自然键锁定读取最新根，与 confirm-booking-by-admin、C10 保持一致
  → 领域依赖 @tournament.match 输入部分与业务动作

- [Q2] 代确认循环内是否只处理仍为 PENDING 的参与者，已被记为 REJECTED 的参与者保持原状不覆盖？
  > 只处理 PENDING：与 flow 层已确认口径一致，REJECTED 参与者不覆盖；若因此无法凑齐全员 CONFIRMED，比赛保持 PENDING_CONFIRM，不结算，本次代确认仍按成功返回
  → 业务动作与详细流程

- [Q3] 比赛完成后结算胜负方报名，是否复用 @tournament.entry 的 C6（结算比赛结果）命令，对胜方和负方分别调用一次？
  > 复用 C6：对本场每一名参与者按其对应报名依次发起结算比赛结果命令，传入胜负关系、比赛轮次和完成时间，与自助确认赛果活动一致
  → 领域依赖 @tournament.entry 与业务动作
