# platform-config.global-config-update.activity.query-all-config-view 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 发布后全量视图按什么名录和顺序返回？
  > 按 SystemConfigKey 声明顺序遍历当前64项，忽略库内未登记 key。
  → 已写入业务动作 A1、详细流程第 1 步与边界情况

- [Q2] 当前值、覆盖状态和版本如何组装？
  > enabled global记录用库值且overridden=true；缺失或停用用默认值且false。无记录version=0，有记录返回库内版本。
  → 已写入业务动作 A2-A3 与详细流程第 2-3 步

- [Q3] 查询失败如何影响发布数据库和缓存？
  > 不返回部分视图并使数据库发布事务回滚；先前已重建的JVM缓存没有事务补偿，可能保留未提交值。
  → 已写入异常分支、详细流程第 4 步与边界情况
