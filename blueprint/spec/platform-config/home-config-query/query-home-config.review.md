# platform-config.home-config-query.flow.query-home-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 运营访问如何鉴权，是否还依赖普通用户登录？
  > 由 /system/admin/** 的 AdminApiKeyInterceptor 校验 X-Admin-Key；服务端密钥为空、请求头为空或不匹配均拒绝。该路径排除普通登录鉴权。
  → 接口契约、详细流程第 1 步与 ACCESS_DENIED 分支

- [Q2] 接口固定返回哪三项首页配置，顺序是否稳定？
  > 固定依次返回 HOME_LAYOUT_CONFIG、HOME_TOURNAMENT_POSTER_CONFIG、HOME_POSTER_CONFIG，对应布局、赛事海报区、通用海报；不返回其他配置。
  → 接口契约与详细流程第 2 步

- [Q3] 记录不存在、停用或启用时，当前值、版本与覆盖状态如何返回？
  > 启用记录返回库值/库版本/overridden=true；停用记录回退默认值但保留库版本且 overridden=false；无记录回退默认值、version=0、overridden=false。
  → 详细流程第 3-4 步与异常分支说明

- [Q4] 查询时是否解析或校验 JSON，单项读取失败是否部分返回？
  > 不解析、不校验、不修复 JSON，已启用库值原样返回；任一数据库查询或组装异常终止整个请求，不返回部分三项结果。
  → 详细流程第 5 步与 SYSTEM_ERROR 分支
