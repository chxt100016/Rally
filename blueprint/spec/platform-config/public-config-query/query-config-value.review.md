# platform-config.public-config-query.flow.query-config-value 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 单项接口的登录、key 必填与空白输入规则是什么？
  > GET /system/config 需要普通 Bearer 登录；key 是 Spring 必填 query param，缺失走全局异常。空字符串存在时可进入 SystemConfig 查询，不 trim，通常成功返回 null。
  → 接口契约、详细流程第 1-2 步与鉴权/参数异常分支

- [Q2] 配置值按什么优先级从缓存、global 覆盖和枚举默认值解析？
  > SystemConfig 先查原 key 缓存，再查 global|key，最后按原 key 查 SystemConfigKey 默认值；只缓存 enabled=true 的 DB 记录，值原样返回。
  → 详细流程第 3-5 步与技术线索

- [Q3] 调用方能否查询未登记或带 scope 的配置，是否存在公开白名单？
  > 没有公开白名单或登记限制。普通未登记 key 若无缓存返回 null；传入 scope|key 可直接命中任意作用域已启用缓存记录，包括非 global 配置。
  → 详细流程第 2-4 步与服务边界

- [Q4] 不存在的 key 如何响应，是否返回类型、版本或来源？
  > 未知且无启用缓存的 key 成功返回 data=null。响应只有字符串/null，不带说明、类型、版本、覆盖状态或来源，也不解析值。
  → 接口契约、详细流程第 5-6 步与异常说明
