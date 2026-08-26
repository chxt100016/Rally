# platform-config.public-config-query.activity.query-public-config-map 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 空数组、null 请求体和 null 元素分别如何处理？
  > 空数组返回空映射；缺失或JSON null请求体、数组null元素都会整体 OPERATION_FAILED，不返回部分结果。
  → 已写入活动契约、异常分支、详细流程第 1、5 步

- [Q2] 批量每项的查找与未知值过滤规则是什么？
  > 逐项按原始缓存键、global|key、枚举默认查询；最终null的未知或空键省略。
  → 已写入业务动作 A1-A3 与详细流程第 2-3 步

- [Q3] 重复 key 和返回顺序如何处理？
  > LinkedHashMap 用原输入作键，后续重复覆盖值但不改变首次插入位置；只保留有值项。
  → 已写入活动契约、详细流程第 3-4 步与边界情况
