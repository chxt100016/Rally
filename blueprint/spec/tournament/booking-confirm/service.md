---
id: tournament.booking-confirm
---

## 服务边界

本服务只处理 `POST /tournament/match/schedule-confirm` 中 `confirm=true` 的参赛者确认分支：记录本人接受赛约，全员确认后将比赛推进为 `PENDING_PLAY`，并在关联活动存在且为 `DRAFT` 时改为 `OPEN`。它不拒赛或请求重订，不创建/修改赛约内容，不校验报名状态，不处理赛果或轮次，不发送通知。
