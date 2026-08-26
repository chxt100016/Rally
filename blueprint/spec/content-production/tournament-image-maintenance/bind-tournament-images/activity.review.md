# content-production.tournament-image-maintenance.activity.bind-tournament-images 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 绑定范围与无匹配赛事时如何处理？
  > 按 tournamentId 更新所有匹配的 tour_tournament 记录，不按年份缩小范围；无匹配记录时更新数为 0 但按成功处理，已上传对象保留。
  → 已写入活动契约、异常分支、业务动作 A1 与详细流程第 1、4 步

- [Q2] 重复绑定、并发更新和字段覆盖边界是什么？
  > 每次把 image_path 与 background_path 覆盖为本次固定资源键，其他赛事字段不变；不做旧值比较或版本校验，数据库最终以最后完成的更新为准。
  → 已写入业务动作 A2、详细流程第 2-3 步与边界情况

- [Q3] 数据库更新失败以及手工 SQL 返回之间是什么关系？
  > 更新失败报 SYSTEM_ERROR，七牛对象不补偿删除；仅 maintain-images-with-sync-statement 流程在自动绑定成功后由流程组装 SQL，活动自身不执行也不转义手工 SQL。
  → 已写入异常分支、业务动作 A3、详细流程第 5 步与边界情况
