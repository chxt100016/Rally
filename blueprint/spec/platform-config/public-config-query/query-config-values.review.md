# platform-config.public-config-query.flow.query-config-values 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 批量接口的登录、请求体、数量上限与空数组规则是什么？
  > POST /system/config/batch 需要普通 Bearer 登录；body 直接绑定 List<String>，无 @Valid 和数量上限。[] 成功返回空 LinkedHashMap；缺失 body 或 JSON null 最终按系统异常。
  → 接口契约、详细流程第 1-2、6 步与异常分支

- [Q2] 未知、空白、重复和 null 标识分别如何处理，结果顺序如何确定？
  > 未知和空白 key 得到 null 后省略；重复 key 后写覆盖但 LinkedHashMap 保留首次插入位置；null 元素使 ConcurrentHashMap.get(null) 抛异常并终止整体。结果按各非 null key 首次出现顺序。
  → 详细流程第 4-6 步与异常说明

- [Q3] 批量查询是否允许带 scope 的缓存键，是否限制可公开配置？
  > 与单项相同，无公开白名单；每项先直接查原 key，所以 scope|key 可以取得任意作用域的已启用缓存值。
  → 详细流程第 2-4 步与服务边界

- [Q4] 中途查询异常时是否返回部分映射，成功结果包含哪些元数据？
  > 任一 null key、请求体或缓存读取异常都会由全局异常处理终止，不交付已组装部分映射。成功只返回 key 到原始字符串值的映射，不含类型、版本、覆盖状态或来源。
  → 接口契约、详细流程第 6-7 步与 OPERATION_FAILED 分支
