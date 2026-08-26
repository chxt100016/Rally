# @tour.draw 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 签表的聚合身份是否为 tournamentId+year+drawType，建立后能否更换？
  > 是，复合身份建立后不可修改；来源更新只能补齐或刷新结构字段。
  → 聚合身份、I1 与 C1

- [Q2] 实时来源只有身份没有 size/totalRounds 时能否建立占位签表？
  > 允许建立 PLACEHOLDER，两个结构字段均为空；后续结构来源可一次补齐为 STRUCTURED。
  → 状态、C1 与实时来源边界情况

- [Q3] size 与 totalRounds 的一致性如何约束，来源后退或空值如何处理？
  > 结构化时 size 必须为正的 2 次幂且 totalRounds=log2(size)，两者必须同时更新；空来源不清旧值，不接受只更新其中一个或不一致值。
  → I3、C2 与结构刷新边界情况
