---
id: tournament.unmatched-entry-elimination
---

## 服务边界

本服务由后台运营按 `tournamentId` 批量淘汰激活赛事当前轮次中成员完整、全员为 `WAITING/FROZEN` 且没有成员参加任何在途比赛的参赛单元，终态为整组报名进入 `ELIMINATED`。服务不处理其他轮次、`IN_MATCH`、`PAYING` 或既有终态报名，不修改比赛、赛约和支付事实，不推进赛事轮次、不自动匹配，也不提供恢复、预览或通知能力。
