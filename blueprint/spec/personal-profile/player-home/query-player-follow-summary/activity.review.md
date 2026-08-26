# personal-profile.player-home.activity.query-player-follow-summary 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 粉丝数和关注数的关系方向是什么？
  > following_id 等于目标的行数是粉丝数，follower_id 等于目标的行数是关注数。
  → 已写入业务动作 A1-A2 与详细流程第 1-2 步

- [Q2] isFollowed 以哪两个用户定位？
  > 以当前登录用户为 follower、路径目标为 following 查询关系是否存在。
  → 已写入活动契约、业务动作 A3 与详细流程第 3 步

- [Q3] 无关系、查看本人和读取失败如何处理？
  > 无关系返回零计数与 false；查看本人仍按相同关系查询，数据库若有自关注可为 true；任一读失败终止整页。
  → 已写入异常分支、详细流程第 4 步与边界情况
