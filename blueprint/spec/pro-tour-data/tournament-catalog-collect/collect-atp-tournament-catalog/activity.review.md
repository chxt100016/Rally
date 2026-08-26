# pro-tour-data.tournament-catalog-collect.activity.collect-atp-tournament-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] ATP 真空列表与客户端异常转 null 有何不同？
  > 真正空列表正常跳过并继续WTA；客户端异常返回null，随后转换空指针使流程OPERATION_FAILED且不处理WTA。
  → 已写入活动契约、异常分支与详细流程第 1 步

- [Q2] ATP 字段转换和解析失败如何处理？
  > 强制tour=ATP/status=active，映射名录字段；日期或奖金解析失败变null，若违反DB必填则整批失败。
  → 已写入业务动作 A2、详细流程第 2 步与边界情况

- [Q3] upsert 身份、图片和未出现存量如何处理？
  > 以tournamentId+year匹配，不含tour；更新全部名录字段但保留主图背景，未出现存量不删不失效。
  → 已写入业务动作 A3、详细流程第 3-4 步与边界情况
