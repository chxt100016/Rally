# tournament.unmatched-entry-elimination.flow.eliminate-unmatched-entries 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 未入赛淘汰是否只处理赛事当前轮次的 WAITING/FROZEN 参赛单元？
  > 是。只处理赛事当前轮次中成员完整、全员为 WAITING/FROZEN 且没有成员参加在途比赛的参赛单元。
  → 落入接口契约、详细流程与业务活动：按当前轮次、完整 entryNo 单元和 WAITING/FROZEN 状态筛选。

- [Q2] 是否要求赛事状态必须为 ACTIVE？
  > 是。赛事必须为 ACTIVE 且 currentRound 非空。
  → 落入详细流程与异常分支：校验赛事 ACTIVE 且 currentRound 非空。

- [Q3] 批量淘汰成功是否返回无数据响应，没有候选时也按成功处理？
  > 是。成功返回无数据；没有候选时幂等成功且不产生变更。
  → 落入触发、接口契约与异常分支：无数据响应，无候选幂等成功。
