---
id: tournament.single-match-cancel
---

## 服务边界

本服务由后台运营按 `tournamentId+matchNo` 精确取消一场状态不是 `COMPLETED` 的赛事比赛，物理删除比赛及参与关系，关闭仍为 `DRAFT` 的关联赛约，并把仍为 `IN_MATCH` 的报名释放为 `WAITING`。服务不取消已完成比赛，不修改非草稿赛约，不推进或回退赛事轮次，不结算胜负、不自动重新匹配，也不替代现有按赛事批量撤销未订场比赛的服务。
