---
id: tournament.result-confirm
---

## 服务边界

本服务只处理 `POST /tournament/match/result-confirm` 中 `confirm=true` 的参赛者确认分支：记录本人确认，全员确认后完成比赛、结算胜负方报名并评估赛事轮次。它不修改胜方选择或比分，不处理 `confirm=false` 的拒绝，不发起支付或占用正赛席位，只登记本次有效通知授权而不发送通知。
