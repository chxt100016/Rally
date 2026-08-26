---
id: tournament.result-reject
---

## 服务边界

本服务只处理 `POST /tournament/match/result-confirm` 中 `confirm=false` 的赛果拒绝分支：在拒绝限额内记录理由和本人拒绝，终止比赛、累计本人当前阶段拒绝数，关闭仍为草稿的赛约，并将仍在比赛中的报名回池。它不改写已提交胜方和比分，不结算晋级/淘汰或推进轮次，不立即重新匹配，不保证拒绝通知送达。
