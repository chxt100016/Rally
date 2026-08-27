---
id: tournament.booking-confirm
---

## 服务边界

本服务只处理 `POST /tournament/match/schedule-confirm` 中 `confirm=true` 的参赛者确认分支：关联赛约存在时，仅当其开始时间严格早于确认时间才以 `MEETUP_EXPIRED` 拒绝且不记录确认，两者恰等时继续；校验通过后记录本人接受赛约，全员确认时将比赛推进为 `PENDING_PLAY`，并在关联活动为 `DRAFT` 时改为 `OPEN`。比赛未关联赛约或关联记录缺失时保留原兼容行为，不因缺失阻止确认。它不拒赛或请求重订，不创建/修改赛约内容，不校验报名状态，不处理赛果或轮次，不发送通知。
