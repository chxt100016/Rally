---
id: tournament.unmatched-entry-elimination
---

## 服务边界

本服务由后台运营按 `tournamentId+userId` 淘汰激活赛事当前轮次中的一个指定参赛者；目标报名必须为 `WAITING/FROZEN` 且用户没有参与任何进行中比赛，终态为仅该用户报名进入 `ELIMINATED`。服务不再批量扫描赛事，不联动双打搭档，不处理其他轮次、`IN_MATCH`、`PAYING` 或既有终态报名，不修改比赛、赛约和支付事实，也不自动匹配、通知或退款。
