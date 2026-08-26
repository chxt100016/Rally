# tournament.entry-preference-update.activity.replace-entry-preference 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名状态禁止更新？
  > 仅 ELIMINATED 与 WITHDRAWN 禁止；PAYING、WAITING、FROZEN、IN_MATCH 等其余状态可更新。
  → 已写入活动契约与详细流程第 3 步

- [Q2] 三组偏好是合并还是替换？
  > preferredDistricts、courtAbility、availableTimes 均以本次提交整体替换旧值，不合并。
  → 已写入活动契约、业务动作 A3 与详细流程第 4 步

- [Q3] 能否通过空列表清空偏好？
  > 不能；地区和时间列表在入口至少一项，保存失败则事务回滚保留原偏好。
  → 已写入异常分支与边界情况
