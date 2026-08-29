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

- [Q4] 双打赛事淘汰指定 userId 时，是否只修改该用户报名，不联动淘汰、冻结或通知搭档？
  > 是。一次只淘汰指定 userId，双打搭档报名保持原状，不自动冻结、淘汰或通知搭档。
  → 落入请求范围、详细流程和服务边界。

- [Q5] 判断未入赛时，是否仅把 MATCHED、BOOKING、SCHEDULED、PENDING_PLAY、PENDING_CONFIRM 视为进行中，而 COMPLETED、REJECTED 历史参与关系不阻止淘汰？
  > 是。仅 MATCHED、BOOKING、SCHEDULED、PENDING_PLAY、PENDING_CONFIRM 阻止淘汰；COMPLETED、REJECTED 历史比赛不阻止。
  → 落入未入赛判定和异常分支。

- [Q6] 原 POST /tournament/admin/entry/eliminate-unmatched 是否直接改为必传 tournamentId+userId 的单人契约，不再保留赛事级批量行为？
  > 是。原接口直接改为 tournamentId+userId 必传的单人契约，不保留赛事级批量行为；后台按钮放在参与者对应用户行。
  → 落入接口契约、触发范围和技术线索。
