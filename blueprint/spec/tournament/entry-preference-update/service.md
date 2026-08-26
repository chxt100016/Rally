---
id: tournament.entry-preference-update
---

## 服务边界

本服务只让当前登录用户整组替换本人已有赛事报名的活动区域、订场能力和可比赛时间；实际只禁止 `ELIMINATED` 与 `WITHDRAWN`，因此 `WAITING`、`PAYING`、`FROZEN`、`IN_MATCH` 都可修改。它不创建报名，不联动搭档，不改参赛编号、阶段、轮次或状态，不校验赛事状态，也不重排已有比赛。
