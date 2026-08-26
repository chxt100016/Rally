---
id: tournament.booking-reschedule-request
---

## 服务边界

本服务只处理 `POST /tournament/match/schedule-confirm` 中 `confirm=false`、仅提供重订理由的分支：任一比赛参与者可将 `SCHEDULED` 比赛退回 `BOOKING`，记录最近重订人/理由/时间，并重置全员确认。它不终止比赛、关闭赛约或让报名回池，不改订场人、原赛约内容/关联和原提交时间，不累计拒绝次数，不自行重新提交，不通知。
