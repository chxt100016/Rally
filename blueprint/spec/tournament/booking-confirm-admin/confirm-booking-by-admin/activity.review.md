# tournament.booking-confirm-admin.activity.confirm-booking-by-admin 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 按 tournamentId+matchNo 定位比赛聚合，是否复用 C10 已确立的锁定读取最新根方式？
  > 复用：按自然键锁定读取最新根，与运营终止比赛（C10）的加载方式保持一致，避免与其他并发操作竞争
  → 领域依赖 @tournament.match 输入部分与业务动作 A2

- [Q2] 开放草稿赛约（@meetup.meetup C11）若因已过期（MEETUP_EXPIRED）被拒绝，是否阻断整个代确认操作？
  > 不阻断：与关联赛约不存在或非 DRAFT 同等对待，视为跳过开放，比赛仍成功推进为 PENDING_PLAY
  → 领域依赖 @meetup.meetup 输出部分与边界情况

- [Q3] 一次性代确认循环内对每个仍非 CONFIRMED 的参与者调用 C4，是否需要逐次重新加载和保存聚合？
  > 不需要：在同一次内存加载的聚合上连续对每个参与者调用确认赛约命令，最终以当前 version 统一做一次条件保存，避免多次数据库往返和并发窗口
  → 业务动作 A3/A4 与实现提示
