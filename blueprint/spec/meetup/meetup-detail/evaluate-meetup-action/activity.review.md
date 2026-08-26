# meetup.meetup-detail.activity.evaluate-meetup-action 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] actionState 的优先级如何处理 CLOSED、创建者、FINISHED、报名与未报名？
  > 先 CLOSED：有效参与者为 CLOSED_JOINED，否则 CLOSED；创建者且无其他有效参与者恒 OWNER_EDITABLE（即使 FINISHED）；FINISHED 有参与者时访客分 FINISHED/FINISHED_JOINED/FINISHED_REVIEWED；创建者有他人时 ONGOING_JOINED 或按锁定分钟 OWNER_EDITABLE/LOCKED；非创建者 PENDING_REVIEW、ONGOING_JOINED/JOINED；未报名 ONGOING 或按 joinMode JOIN_DIRECT/APPLY_APPROVAL。
  → 已写入活动契约、业务动作 A1 与详细流程第 1-4 步

- [Q2] 什么时候计算 restrictions，各限制如何叠加？
  > 仅 actionState=JOIN_DIRECT/APPLY_APPROVAL 时读取当前 UserProfile 并同时收集，全部可叠加：资料默认/档案缺失三类、FULL、性别未知/男限/女限、水平不符、信誉分低；空列表表示 joinable=true，其他状态 joinable/restrictions 均 null。
  → 已写入活动契约、业务动作 A2-A4、详细流程第 5-8 步

- [Q3] 资料或配置缺失时，水平、性别、信誉和编辑锁定如何降级？
  > 用户或必要资料读取失败归 SYSTEM_ERROR。levelMode null 或 tennis_profile/ntrp null 视为水平符合；genderLimit ANY 或用户 gender null 视为符合，但 UNDISCLOSED 在受限时形成 GENDER_UNKNOWN；reputation null 视为符合，否则与 meetup.join.min_reputation_score 比较。创建者编辑锁读 meetup.edit.lock_minutes_before_start。
  → 已写入 reads、异常分支、详细流程第 3、6-7 步与边界情况
