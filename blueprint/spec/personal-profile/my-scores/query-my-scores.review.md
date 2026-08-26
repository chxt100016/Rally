# personal-profile.my-scores.flow.query-my-scores 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] matchType 为空或指定时，RALLY 比分如何筛选，meetupId 空字符串如何处理？
  > matchType 为 null 时不筛选，包含 RALLY；命令枚举只有 SINGLE、DOUBLE，指定其中之一只保留对应记录，无法单独筛 RALLY。meetupId 只有 null 表示不筛选，空字符串会按等于空串过滤。
  → 已在契约和详细流程明确空类型包含 RALLY、指定类型仅单双打，以及 meetupId 只有 null 才不筛选。

- [Q2] lastId 的真实编码格式是什么，非法、类型错误或不在筛选结果中如何处理？
  > lastId 实际是 URL-safe Base64 无填充编码的 JSON 数组，首项应为比分 bizId 字符串。空白、Base64/JSON 非法或空数组回首页；首项非字符串会 ClassCastException；合法但当前筛选集找不到也从索引 0 回首页。
  → 已在契约、详细流程、异常说明和技术线索明确游标编码及非法、类型错误、找不到三种结果。

- [Q3] pageSize 的默认值与 0、负数、极大值分别如何表现？
  > null 默认 20。0 时若有数据，取 1 条探测后返回空 list、hasMore=true、nextCursor=null；负数会在 subList 范围计算时报错；Integer.MAX_VALUE 加 1 溢出为负并报错。没有上限保护。
  → 已在契约、详细流程和异常分支明确默认 20、零页、负数与溢出行为。

- [Q4] 本人同时出现在两侧或 winSide 异常时如何确定本人视角与胜负？
  > 先检查 A1/A2，因此同时在两侧按 A 侧构造视角。A 侧且 winSide=A 或非 A 侧且 winSide=B 才为 WIN，其余包括空或未知 winSide 都为 LOSE，不从分数纠正。
  → 已在详细流程、异常说明和技术线索明确 A 侧优先与异常胜方计负。

- [Q5] 分页结果返回哪些排序、日期、人员资料与游标字段，是否返回总数和盘号？
  > 源记录按 bizId 降序；日期为 meetupDate 的 MM-dd。返回 bizId、meetupId、胜负、类型/盘制、本人和对手分数、抢七、本人性别、队友及对手快照和签名头像。PageDTO.total 永远 null，不返回 setNum；有更多且本页非空才返回下一游标。
  → 已在契约和技术线索明确排序、日期、快照资料、total、盘号与下一游标口径。
