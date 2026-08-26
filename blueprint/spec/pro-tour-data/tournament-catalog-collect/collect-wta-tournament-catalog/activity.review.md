# pro-tour-data.tournament-catalog-collect.activity.collect-wta-tournament-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] WTA null/空响应如何推进，ATP 提交受影响吗？
  > null或空内容正常跳过；ATP已独立提交并保留。只有WTA转换/保存异常回滚WTA自身。
  → 已写入活动契约、异常分支与详细流程第 1、4 步

- [Q2] WTA 状态、级别和奖金怎样转换？
  > past映射completed否则active；Grand Slam映射GS，其他去WTA前缀；long奖金直接缩窄int，展示文本保留数值币种。
  → 已写入业务动作 A2、详细流程第 2-3 步与边界情况

- [Q3] WTA 的身份冲突和成功响应是什么？
  > 仍以tournamentId+year不含tour，可能覆盖同号ATP；成功或空跳过都返回空响应体，不给统计。
  → 已写入业务动作 A3、详细流程第 4-5 步与边界情况
