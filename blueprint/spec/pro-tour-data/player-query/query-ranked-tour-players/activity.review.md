# pro-tour-data.player-query.activity.query-ranked-tour-players 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] tour 参数如何规范及空白如何处理？
  > 空白直接空列表；非空只转大写不trim，不限制ATP/WTA，再精确查询。
  → 已写入业务动作 A1、详细流程第 1 步与边界情况

- [Q2] 排名查询、姓名、国家和年龄如何组装？
  > rank非null按升序全量；姓名名+空格+姓trim，国家内置映射否则原码，年龄按本地今日Period计算，未来日期可负。
  → 已写入业务动作 A2-A3、详细流程第 2-3 步与边界情况

- [Q3] 返回字段和翻译缺口是什么？
  > 返回id/rank/name/country/points/age/birthDate；完整姓名命中非空ZH_CN替换，未命中保留并输出登记键。
  → 已写入活动契约、业务动作 A4 与详细流程第 4-5 步
