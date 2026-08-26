---
id: tournament.tournament-abandon
---

## 服务边界

本服务只由后台共享 API Key 调用方把存在的 `DRAFT` 或 `ACTIVE` 赛事事务性改为 `ABANDONED`，重复废弃拒绝。它不使用废弃原因，不写结束时间或操作人，不删除赛事，不处理配置、报名、比赛、赛约、支付、线下活动、退款和通知，也不支持恢复。
