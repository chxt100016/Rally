# pro-tour-data.tournament-query.activity.query-tournament-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 筛选值和日期范围如何解释？
  > status 仅识别大写 FINISHED/ONGOING/UPCOMING，range 不区分大小写识别 recent/live；未知值不限制对应维度，type 原样匹配。
  → 已写入活动契约、详细流程第 1 步与边界情况

- [Q2] 类别过滤和展示分组的准确规则是什么？
  > 只排除可解析为整数且小于 250 的类别；新赛事仅与组首项按城市忽略大小写且赛期相交比较，实际不比较名称。
  → 已写入业务动作 A2-A3、详细流程第 2-3 步与边界情况

- [Q3] 响应身份、年份和背景图如何处理？
  > id 仅为外部 tournamentId 且不返回年份；背景键非空即生成 3600 秒签名地址，不校验对象存在。
  → 已写入详细流程第 4-5 步与边界情况
