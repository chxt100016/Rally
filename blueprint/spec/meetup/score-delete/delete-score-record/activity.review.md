# meetup.score-delete.activity.delete-score-record 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 删除权限、实际阶段与复盘截止时间如何判定？
  > 发布者或有 PENDING/JOINED/REVIEWED/SKIPPED 报名可删；实际状态须 ONGOING/FINISHED，且不晚于 endTime+review.deadline_days。
  → 已写入异常分支、领域依赖、业务动作 A1 与详细流程第 1 步

- [Q2] 是否预读比分并校验记录人、阵容、版本或盘号？
  > 不预读记录，不校验 recordedBy、球员阵容、version 或 setNumber；有约球复盘资格即可按 meetupId+bizId 删除任何比分。
  → 已写入业务动作 A2、详细流程第 2-3 步与边界情况

- [Q3] 零行、重复、物理删除审计、并发与评分重算如何处理？
  > 物理删除且无墓碑/删除审计，零行与重复删除均成功；不返回影响数，并发更新删除由执行顺序决定，评分重算仅 TODO。
  → 已写入活动契约、业务动作 A3、详细流程第 3-5 步、边界情况与实现提示
