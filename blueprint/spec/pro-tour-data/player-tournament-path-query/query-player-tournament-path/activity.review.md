# pro-tour-data.player-tournament-path-query.activity.query-player-tournament-path 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 签表与球员如何定位，缺失和跨tour重复如何处理？
  > 签表按tournamentId+year+drawType唯一查，球员仅按playerId唯一查；缺失返回data=null，跨tour同号重复可能唯一查询失败。
  → 已写入活动契约、业务动作 A1、详细流程第 1 步与边界情况

- [Q2] 晋级、出局、下一场的判定规则是什么？
  > 比赛roundNumber升序null当0；FINISHED且本人胜进路径，其他FINISHED为败局且最后败局作出局。未出局优先首个未完成有位置比赛，否则从最后胜局相邻子树选未淘汰最小种子。
  → 已写入业务动作 A2-A3 与详细流程第 2-4 步

- [Q3] 潜在路径、种子和翻译有什么跨域/字段限制？
  > 后续沿matchIndex二叉树选对手子树最高种子；种子从同tournamentId全报名合并不隔离年份签表；next球场译文只替换排期文本，court字段保留原文。
  → 已写入业务动作 A3-A4、详细流程第 5-6 步与边界情况
