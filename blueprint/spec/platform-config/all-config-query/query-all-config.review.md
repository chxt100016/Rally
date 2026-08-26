# platform-config.all-config-query.flow.query-all-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 配置清单的来源、顺序及未注册数据库记录如何处理？
  > 以 SystemConfigKey.values() 为唯一清单，按枚举声明顺序返回当前 64 项；库中未注册键不返回。
  → 详细流程第 2 步、接口契约与技术线索

- [Q2] 配置记录不存在、停用或启用时，生效值、版本和 overridden 分别如何返回？
  > 启用记录返回库值、库版本且 overridden=true；停用记录回退默认值、保留库版本且 overridden=false；无记录回退默认值、version=0 且 overridden=false。
  → 详细流程第 3-4 步与异常分支说明

- [Q3] 运营后台访问具体如何鉴权，是否还要求用户登录？
  > 由 /system/admin/** 的 AdminApiKeyInterceptor 校验 X-Admin-Key；服务端配置为空、请求头为空或不匹配都拒绝。该路径排除普通登录鉴权，不要求用户登录。
  → 触发、接口契约、详细流程第 1 步及 ACCESS_DENIED 分支

- [Q4] 查询是否校验或归一化库内配置值，失败时是否允许部分返回？
  > 不校验或归一化库值，原样返回；任一单项数据库查询或组装异常都会使整个请求失败，没有部分成功结果。
  → 详细流程第 5 步与 SYSTEM_ERROR 分支
