# Tennis 到 Tour 重命名总结

## 重命名范围

### 1. 包名变更
- `com.rally.web.tennis` → `com.rally.web.tour`
- `com.rally.wechat.tennis` → `com.rally.wechat.tour`
- `com.rally.tennis` → `com.rally.tour`
- `com.rally.domain.tennis` → `com.rally.domain.tour`
- `com.rally.db.tennis` → `com.rally.db.tour`
- `com.rally.client.tennistv` → `com.rally.client.tourtv`

### 2. 类名变更（主要类）
- `TennisCollectController` → `TourCollectController`
- `TennisMatchController` → `TourMatchController`
- `TennisPlayerQueryController` → `TourPlayerQueryController`
- `TennisQueryController` → `TourQueryController`
- `TennisUploadController` → `TourUploadController`
- `TennisContentController` → `TourContentController`
- `TennisPromptController` → `TourPromptController`
- `TennisCollectJob` → `TourCollectJob`
- `TennisCollectFacade` → `TourCollectFacade`
- `TennisContentAppService` → `TourContentAppService`
- `TennisMatchAppService` → `TourMatchAppService`
- `TennisPlayerQueryService` → `TourPlayerQueryService`
- `TennisPromptService` → `TourPromptService`
- `TennisUploadAppService` → `TourUploadAppService`
- `TennisMatchQueryDomainService` → `TourMatchQueryDomainService`
- `TennisTranslationService` → `TourTranslationService`
- `TennisTvClient` → `TourTvClient`

### 3. 数据库表名变更
- `tennis_player` → `tour_player`
- `tennis_tournament` → `tour_tournament`
- `tennis_draw` → `tour_draw`
- `tennis_tournament_entry` → `tour_tournament_entry`
- `tennis_match` → `tour_match`
- `tennis_set_score` → `tour_set_score`
- `user_tennis_profile` → `user_tour_profile`

### 4. 字段名变更
- `tennisMatchId` → `tourMatchId`
- `tennisProfileGateway` → `tourProfileGateway`
- `tennisProfileService` → `tourProfileService`
- `tennisProfileRepository` → `tourProfileRepository`
- `tennisTvClient` → `tourTvClient`
- `tennisEntryGateway` → `tourEntryGateway`
- `tennisPlayerGateway` → `tourPlayerGateway`
- `tennisMatchCollectGateway` → `tourMatchCollectGateway`
- `tennisDrawGateway` → `tourDrawGateway`
- `tennisTournamentGateway` → `tourTournamentGateway`
- `tennisTranslationService` → `tourTranslationService`
- `tennisMatchQueryDomainService` → `tourMatchQueryDomainService`
- `tennisContentAppService` → `tourContentAppService`
- `tennisPromptService` → `tourPromptService`
- `tennisPlayerQueryService` → `tourPlayerQueryService`
- `tennisMatchAppService` → `tourMatchAppService`
- `tennisCollectFacade` → `tourCollectFacade`

### 5. URL 路径变更
- `/tennis/*` → `/tour/*`

### 6. 配置项变更
- `job.tennis.*` → `job.tour.*`

## 修改的文件统计
- 总计修改文件数：179 个
- 涉及模块：rally-adapter, rally-app, rally-domain, rally-infrastructure, start, docs

## 生成的 SQL 文件
- `docs/sql/rename_tennis_to_tour.sql` - 数据库表重命名语句
- `docs/sql/tour_tables.sql` - 更新后的建表语句
- `docs/api/tour-match.md` - 更新后的 API 文档

## 注意事项
1. 外部 API URL（如 tennistv.com, wtatennis.com）保持不变
2. 所有 Java 文件中的包名、类名、字段名、变量名已更新
3. 配置文件中的配置项已更新
4. SQL 文件中的表名、索引名已更新
5. 文档文件中的引用已更新

## 执行顺序建议
1. 先执行 `docs/sql/rename_tennis_to_tour.sql` 重命名数据库表
2. 然后部署更新后的代码
3. 验证所有功能正常工作
