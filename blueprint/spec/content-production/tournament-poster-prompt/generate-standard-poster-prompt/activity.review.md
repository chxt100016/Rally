# content-production.tournament-poster-prompt.activity.generate-standard-poster-prompt 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 赛事不存在、同编号多条记录和返回空值如何处理？
  > 按 tournamentId 取现有查询第一条，不指定年份或排序；未找到时成功返回 null/空结果，不报业务错误。
  → 已写入活动契约、业务动作 A1、详细流程第 1 步与边界情况

- [Q2] category 与 surface 的空白、大小写、未知值分别如何处理？
  > category trim 后按 GS/1000/500/250/final/finals 映射显示名和角度，匹配角度时兼容大小写；未知级别原样显示并用默认中角度。surface trim 并转小写映射常见场地，未知值原样，城市 trim 后空则省略。
  → 已写入业务动作 A2、详细流程第 2-3 步与边界情况

- [Q3] 提示词固定约束、读取范围与副作用是什么？
  > 读取 tour_tournament 的 tournament_id、name、category、surface、city；输出固定包含场地、中央球场、城市、级别、避权和 16:9 规则，再追加赛事资料和角度。不写库、不缓存、不登记翻译。
  → 已写入 reads、业务动作 A3、详细流程第 4-5 步与实现提示
