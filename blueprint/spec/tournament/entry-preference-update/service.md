---
id: tournament.entry-preference-update
---

## 服务边界

本服务只让当前登录用户整组替换本人已有赛事报名的活动区域、订场能力和可比赛时间；禁止 `CHAMPION`、`ELIMINATED` 与 `WITHDRAWN`，`WAITING`、`PAYING`、`FROZEN`、`IN_MATCH` 等非终态可修改。地区与时间列表只要求集合非空，元素按请求原样保存，不清洗、去重或新增格式校验。它不创建报名，不联动搭档，不改参赛编号、阶段、轮次或状态，不校验赛事状态，也不重排已有比赛。
