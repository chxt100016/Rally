# platform-config.all-config-query.activity.query-all-config-view 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 返回配置的名录、顺序和数量由什么决定？
  > 只遍历 SystemConfigKey 枚举，按声明顺序返回，当前64项；数据库中未注册 key 不进入结果。
  → 已写入活动契约、业务动作 A1、详细流程第 1 步与边界情况

- [Q2] 记录不存在、停用和启用时当前值与 overridden 如何确定？
  > 只查 global；存在且启用用库值、overridden=true，不存在或停用回退枚举默认值并标记 false。
  → 已写入业务动作 A2-A3 与详细流程第 2-3 步

- [Q3] version 与非法配置值如何返回？
  > 无记录 version=0；有记录返回库内 version，即使停用。值不解析、不校验、不修复，启用非法值也原样展示。
  → 已写入详细流程第 4-5 步与边界情况

- [Q4] 全量配置活动的 A1 是否按 SystemConfigKey.values() 动态遍历，不再写死当前项数？
  > 是，按 SystemConfigKey.values() 的声明顺序动态遍历全部正式配置项，不承诺固定数量。
  → 落实到时序图、A1 详细流程与边界情况。

- [Q5] 首页旧配置记录退出名录后，活动是否只查询正式名录中的 global 记录并忽略其他数据库行？
  > 是，只为正式名录项查询 global 记录，未登记的旧数据库行不进入结果。
  → 落实到 A1、A2 的查询范围与详细流程。

- [Q6] 独立全量查询与全局配置更新后的回显是否可以复用同一个 QueryAllConfigViewActivity，并保持默认值、版本与 overridden 规则一致？
  > 可以，两个服务复用同一个 QueryAllConfigViewActivity；记录缺失时版本为 0，停用记录回退默认值但保留库内版本。
  → 落实到活动契约、A3 详细流程与实现映射。
