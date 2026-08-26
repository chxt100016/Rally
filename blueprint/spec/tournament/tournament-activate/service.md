---
id: tournament.tournament-activate
---

## 服务边界

本服务只由后台共享 API Key 调用方把存在且时间配置满足最小条件的 `DRAFT` 赛事事务性改为 `ACTIVE`。它不复核完整创建配置或当前时间，不修改其他字段，不创建报名/比赛，不启动匹配，也不返回赛事详情。
