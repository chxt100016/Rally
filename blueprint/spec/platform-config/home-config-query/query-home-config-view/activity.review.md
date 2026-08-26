# platform-config.home-config-query.activity.query-home-config-view 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 三项配置的范围和顺序是什么？
  > 固定依次为 home.layout.config、home.tournament.poster.config、home.poster.config，不遍历其他配置。
  → 已写入活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 启用、停用、缺失记录如何确定值与 overridden？
  > 启用用库值且true；停用或缺失回退枚举默认且false。
  → 已写入业务动作 A2 与详细流程第 2 步

- [Q3] 版本和非法 JSON 如何返回？
  > 无记录version0，有记录返回库内版本即使停用；查询不解析JSON，启用非法内容原样返回。
  → 已写入业务动作 A3、详细流程第 3-4 步与边界情况
