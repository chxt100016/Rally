---
id: tournament.result-confirm
---

## 服务边界
本服务只处理 `POST /tournament/match/result-confirm` 中 `confirm=true` 的参赛者确认分支：记录本人确认，全员确认后完成比赛并统一结算。非决赛完成时结算胜负报名并评估赛事轮次；决赛完成时将胜方报名置为冠军，并把赛事置为 `FINISHED`、记录冠军报名编号和结束时间。它不修改胜方选择或比分，不处理 `confirm=false` 的拒绝，不发送通知，也不发起支付或占用正赛席位。
