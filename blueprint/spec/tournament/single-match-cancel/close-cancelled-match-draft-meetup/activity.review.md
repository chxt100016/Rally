# tournament.single-match-cancel.activity.close-cancelled-match-draft-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 关联赛约是否只在存储状态为 DRAFT 时关闭？
  > 是。只关闭存储状态为 DRAFT 的关联赛约。
  → 落入领域依赖和业务动作：只调用 DRAFT 关闭命令。

- [Q2] meetupId 为空、赛约缺失或非 DRAFT 时是否幂等跳过？
  > 是。meetupId 为空、赛约缺失或非 DRAFT 均幂等跳过。
  → 落入详细流程与边界情况：空、缺失和非 DRAFT 跳过。

- [Q3] 草稿赛约关闭失败是否回滚比赛删除和报名释放？
  > 是。保存 DRAFT→CLOSED 失败时回滚整次单场取消事务。
  → 落入异常分支与详细流程：失败回滚外层事务。
