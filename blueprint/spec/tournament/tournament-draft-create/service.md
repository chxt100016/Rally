---
id: tournament.tournament-draft-create
---

## 服务边界

本服务只由持有后台共享 API Key 的调用方校验赛事配置、补充城市名并事务性建立 `DRAFT`/`QUALIFIER` 自办赛事，返回新赛事编号。`offlineFromRound` 可空，空表示全部轮次线上完成。它不校验名称唯一，不验证图片对象，不记录具体运营人，不激活赛事、开放报名或创建报名、比赛、赛约、支付和线下活动。
