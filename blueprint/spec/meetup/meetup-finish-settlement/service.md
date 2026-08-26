---
id: meetup.meetup-finish-settlement
---

## 服务边界

本服务每日批量把结束时间已过且存储状态为 `OPEN` 或小写 `full` 的普通、赛事约球置为 `FINISHED`。它不持久化进行中状态，不处理 `ONGOING` 或大写 `FULL`，不修改报名、比赛、费用、群聊、比分与评价，也不发送结束通知；异常只记录日志并等待后续调度重新覆盖。
