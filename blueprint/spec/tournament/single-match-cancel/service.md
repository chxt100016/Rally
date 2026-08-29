---
id: tournament.single-match-cancel
---

## 服务边界

本服务由后台运营按 `tournamentId+matchNo` 精确终止一场状态不是 `COMPLETED` 的赛事比赛，复用现有 `REJECTED` 终态并保留比赛根、参与者和全部历史字段；同时关闭仍为 `DRAFT` 的关联赛约，并把仍为 `IN_MATCH` 的报名释放为 `WAITING`。服务不新增比赛状态或终止审计字段，不删除比赛与参与者，不修改非草稿赛约，不结算胜负、不自动重新匹配，也不改变现有批量取消未订场比赛的物理删除语义。
