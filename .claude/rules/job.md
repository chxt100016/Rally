## 定时任务
1. 定时 Job 在 `rally-adapter/src/main/java/com/rally/job`
2. 任务开关通过 `job.[业务key].enabled` 控制（prod 为 `true`，dev 默认不启用），cron 表达式在 `application-prod.yml` 中配置。