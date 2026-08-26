# platform-config.home-config-update.flow.update-home-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 请求允许修改哪些 key，鉴权和空值要求是什么？
  > X-Admin-Key 独立鉴权；仅三项 HOME_* key，key 非空白、configValue/version 非 null。其他已登记 key 也以该配置不允许在首页配置中心修改拒绝。
  → 接口契约、详细流程第 1-2 步与访问/范围异常分支

- [Q2] 三类首页 JSON 的结构、数量、标识、类型和必填字段如何校验？
  > 布局数组最多30区；id 非空唯一、最长64且限字母数字_-；type 限六种，非 POSTER 不可同类重复。POSTER 需标题和海报；赛事海报对象需标题/副标题/海报；通用海报为数组。每组最多20张，每张 type 限 NAVIGATE/PREVIEW 且 image 非空。
  → 详细流程第 3-5 步与 PARAM_ERROR 分支

- [Q3] 哪些字段或引用不会校验，空区域和空海报列表是否可发布？
  > 空布局数组和空海报数组允许。不会验证 enabled/cityAware 的类型、普通动态区的其他必填字段、海报标题/副标题/跳转目标，也不检查图片 key 或跳转目标真实可用。未知额外字段随 JSON 重序列化保留。
  → 详细流程第 5 步与异常分支补充说明

- [Q4] 首次、已有和停用记录如何做版本控制，成功响应包含什么？
  > 无记录只接受 version=0 并建 enabled=true/version=1/json；已有含停用记录按 id+version 条件更新、重新启用并 version+1，不更新 valueType。成功固定返回三项首页配置最新视图。
  → 接口契约、详细流程第 6、8 步与 OPERATION_FAILED 分支

- [Q5] 缓存刷新、多实例和事务失败后的状态如何处理？
  > 保存后、事务提交前 SystemConfig.init 仅刷新当前 JVM。数据库回滚不会回滚静态缓存；刷新失败可能留空/部分缓存，刷新成功后若响应查询或提交失败可能保留未提交值；其他实例不会即时同步。
  → 详细流程第 7-8 步、SYSTEM_ERROR 分支与服务边界
