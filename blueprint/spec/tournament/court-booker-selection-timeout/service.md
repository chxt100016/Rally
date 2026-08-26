---
id: tournament.court-booker-selection-timeout
---

## 服务边界

本服务在超时 Job 启用时，批量终止匹配后超过三天仍为 `MATCHED` 的比赛，写入 `REJECTED/TIMEOUT`，关闭异常关联且仍为草稿的赛约，并将仍为 `IN_MATCH` 的报名回到 `WAITING`。它不处理其他比赛阶段超时，不计拒绝次数，不改轮次或参与者确认状态，不立即重新匹配，不发送通知；单场失败不阻断其他场。
