# tour_match 比分 JSON 上线说明

## 发布前

1. 备份 `tour_match` 和 `tour_set_score`，暂停比赛采集任务，避免回填期间继续写旧表。
2. 执行 `docs/sql/check_tour_match_sets_json.sql` 中的迁移前只读检查。
3. 人工复核 winner 异常、非 LIVE/FINISHED 的旧比分，以及已知污染记录 `2775`、`2776`。

## 数据与代码切换

1. 执行 `docs/sql/migration_tour_match_sets_json.sql` 的 `ALTER TABLE`。
2. 执行同文件的安全回填；该语句不会覆盖已有 `sets_json`，也不会迁移已知污染记录。
3. 再执行只读检查 SQL，核对 JSON 数量、每场盘数和污染记录。
4. 发布应用。新版本只读写 `tour_match.sets_json`，不再访问 `tour_set_score`。
5. 恢复采集，抽查 Live、Finished、PlayerTournament 和赛事比赛查询的响应格式与比分方向。

## 观察与回滚

- 观察期内保留 `tour_set_score`，不要 DROP，也不要立即改名。
- 应用需要回滚时可直接回滚代码；旧表仍在，但新版本运行期间产生的最新比分只存在于 `sets_json`，恢复旧采集前应重新采集或单独同步这段数据。
- 稳定后可经明确授权执行：

```sql
RENAME TABLE tour_set_score TO tour_set_score_backup_202608;
```

- 备份表的最终删除必须单独审批，不包含在本次迁移中。
