---
id: tournament.result-confirm.flow.confirm-result
type: api
facade: POST /tournament/match/result-confirm
---

## 概要

记录本人赛果确认，全员确认后完成比赛；决赛产生冠军并结束赛事。

## 触发

已提交赛果的比赛参与者要接受当前胜方结论时，以 `confirm=true` 发起。

## 接口契约

请求体包含非空 `matchId` 和非空 `confirm`；本流程要求 `confirm=true`。后端不接收订阅相关字段。

## 业务活动

- confirm-result  记录本人确认，并在全员确认时完成比赛与晋级结算

## 流程图

```mermaid
flowchart TD
    A[confirm-result 确认赛果] -->|尚有未确认者| W([保持待确认])
    A -->|全员已确认| C[完成比赛与结算]
    C -->|决赛| F[记录冠军并结束赛事]
    C -->|非决赛| R[评估轮次推进]
    A -->|状态、身份或并发冲突| E[业务失败]
    F --> S([返回成功])
    R --> S
```

## 详细流程

1. 识别当前登录用户，接收非空比赛编号和 `confirm=true`。
2. 取得比赛及参与者，确认比赛为 `PENDING_CONFIRM`，所属赛事、本人报名和本人参与关系存在。
3. 将本人赛果确认状态设为 `CONFIRMED` 并记录当前时间；若尚有未确认者，保存后结束。
4. 若全员已确认，确认胜方参赛编号存在，将比赛改为 `COMPLETED` 并记录完成时间。
5. 结算胜负方报名：资格赛胜方进入 `PAYING`、负方回 `WAITING`；非决赛正赛胜方进入下一轮 `WAITING`、负方进入 `ELIMINATED`；决赛胜方进入 `CHAMPION`、负方进入 `ELIMINATED`。
6. 若已完成比赛的轮次为 `FINAL`，将所属赛事更新为 `FINISHED`，写入 `championEntryNo=winnerEntryNo` 和 `endTime=completedTime`；否则按已完成场数评估赛事轮次推进。
7. 返回成功；本流程不登记订阅信息或发送通知。

## 异常分支

| 对外失败码 | 触发条件 | 由哪个活动报出 | 补偿动作或超时处理 | 对外提示 |
|---|---|---|---|---|
| 未登录/参数校验错误 | 无有效登录，比赛编号空白，或 `confirm` 未提供 | 入口鉴权与校验 | 不修改 | 统一登录提示／比赛ID不能为空／确认状态不能为空 |
| `TOURNAMENT_ENTRY_NOT_FOUND` | 比赛不存在、本人报名不存在，或本人不属于参与者 | confirm-result | 事务回滚 | 报名记录不存在 |
| `TOURNAMENT_NOT_FOUND` | 比赛所属赛事不存在 | confirm-result | 事务回滚 | 赛事不存在 |
| `TOURNAMENT_INVALID_RESULT_CONFIRM` | 比赛不是 `PENDING_CONFIRM` | confirm-result | 所有对象不变 | 当前状态不允许确认结果 |
| `TOURNAMENT_RESULT_WINNER_REQUIRED` | 全员确认时没有胜方参赛编号 | confirm-result | 事务回滚本次确认 | 请选择获胜方 |
| `TOURNAMENT_MATCH_VERSION_CONFLICT` | 保存前比赛已被并发修改 | confirm-result | 事务回滚，保留先完成的变化 | 比赛状态已变更，请刷新后重试 |
| `OPERATION_FAILED` | 参与关系、报名、比赛、赛事轮次或冠军结算保存失败 | confirm-result | 事务回滚，不交付部分结果 | 系统异常，请稍后重试 |

## 技术线索

- HTTP：`POST /tournament/match/result-confirm`
- 请求：`ResultConfirmCmd`，本流程仅 `confirm=true`
- 调用：`TournamentMatchAppService.confirmResult()` → `ConfirmResultActivity.execute()` → `TournamentMatch.confirmResult()` → 非决赛 `TournamentRoundProgressDecisionService.evaluate()` 后条件推进／决赛 `Tournament.finish()`
- 并发：`TournamentMatchRepository.updateWithVersion()`
- 事务：比赛、参与确认、报名、冠军和赛事终态在同一事务提交。
