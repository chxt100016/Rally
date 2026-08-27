---
id: "@meetup.finish-settlement"
kind: service
reads:
  - name: rally_meetup
    columns: [biz_id, status, end_time]
---

## 概要

用一条批量更新把主线指定存储状态且结束时间已过的约球置为结束。

## 职责

| 范围 | 内容 |
|---|---|
| 做什么 | 在构造更新时取得当前时间，精确匹配存储状态 `OPEN` 或小写 `full` 且 `end_time` 早于当前时间的记录，批量改为 `FINISHED` 并返回影响行数 |
| 不做什么 | 不先读取候选清单，不逐聚合复核，不匹配 `ONGOING` 或大写 `FULL`，不发送通知、不重试、不记录日志 |
| 为什么不是聚合 | 这是跨任意数量约球的定时兜底批量迁移，没有单个约球的加载与保存边界 |
| 事务边界 | 单条数据库 UPDATE 自身原子；更新异常整体抛出，由调度入口记录，本服务不拆分逐条结果 |

## 契约

### 输入

无。结算基准在构造批量更新时由运行环境 `LocalDateTime.now()` 取得。

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| affectedCount | Integer | 本次由单条更新改为 `FINISHED` 的记录数，可为 0 |

## 规则

R1 只精确匹配存储状态字符串 `OPEN` 或小写 `full`；`ONGOING`、大写 `FULL`、`CLOSED`、`FINISHED` 及其他值均不匹配。
R2 只匹配 `end_time < LocalDateTime.now()`；结束时间为空、晚于或恰等于构造更新时的当前时间均不匹配。
R3 命中记录由同一条 UPDATE 直接设为大写 `FINISHED`，不先返回候选，也不逐聚合执行结束命令。
R4 返回数据库影响行数；没有命中记录时返回 0 并成功结束。
R5 数据库更新异常原样传播；服务不在本轮重试，也不提供部分成功明细。

## 边界情况

- 已结算记录不会再次匹配，重复调度对已完成记录无额外影响。
- 任务停用期间不补建结算凭据；重新启用后的下一次执行处理当时仍符合条件的积压记录。
- 状态大小写不做归一化；只有主线历史口径中的 `OPEN` 和 `full` 命中。
- 结束时间恰等于更新构造时刻不命中，等待下一次调度。
- 更新执行前并发改变状态或结束时间时，由该单条 UPDATE 的 WHERE 条件在数据库执行时决定是否命中。

## 实现提示

沿用 `MeetupRepository.batchUpdateToFinished()` 与现有 `MeetupService.batchUpdateToFinished()`：在构造 `LambdaUpdateWrapper` 时取当前时间，使用 `status IN ('OPEN','full')`、`end_time < now`，并设置 `status='FINISHED'`。不要改为候选读取、枚举归一化或逐聚合迁移。
