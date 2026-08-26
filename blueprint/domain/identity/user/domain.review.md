# @identity.user 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] user 与 user_tennis_profile 应是两个聚合，还是一个用户聚合内的根与可选实体？
  > 归为一个聚合：user 是根，user_tennis_profile 是每个用户至多一份的可选实体。档案状态、NTRP、视频及核查字段通过用户根修改。
  → 聚合清单、边界与 I2

- [Q2] 重复提交初始档案是否允许覆盖 NORMAL 或 UNDER_REVIEW，并重置评分或核查状态？
  > 不允许。完成初始档案只接受 NONE 或 TBC；NONE 时先在同一命令内建立档案。NORMAL 与 UNDER_REVIEW 均拒绝重复初始化，避免重置评分或破坏核查状态。
  → 状态、C5 与初始档案重复提交边界情况

- [Q3] 档案视频应具备哪些聚合内约束，删除规则按删除前还是删除后数量判断？
  > 视频 key 必须非空、属于 videos/{userId}/ 目录并在列表内唯一，硬上限 5；NORMAL 和 UNDER_REVIEW 至少保留 1 项。删除按移除全部匹配项后的结果校验，不能删至空。
  → I4、I5 与 C6-C8

- [Q4] status、is_under_review 与 review_remaining_matches 如何保持一致，自评未再次触发核查时如何处理既有核查期？
  > UNDER_REVIEW 必须同时 isUnderReview=true 且 remainingMatches>0；NORMAL/TBC 必须标记 false 且剩余场次为空。自评未达到新触发阈值时保留既有 UNDER_REVIEW，不得用 NORMAL 覆盖。
  → 状态、I3、C9 与自评边界情况
