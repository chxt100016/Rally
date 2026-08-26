# tournament.tournament-list.activity.query-tournament-admin-list 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 筛选和分页排序规则是什么？
  > 城市/NTRP 非空白精确匹配，状态精确；createTime 倒序页码分页，无稳定次级排序。
  → 已写入活动契约、详细流程第 1-2 步与边界情况

- [Q2] hasMore 和超界页如何计算？
  > pageNum*pageSize<total；超界返回空列表、真实 total、hasMore=false。
  → 已写入异常分支与详细流程第 3 步

- [Q3] 图片签名失败是否返回部分页？
  > 不返回；非空键签 3600 秒且不验对象，任一签名异常使整页失败。
  → 已写入异常分支、详细流程第 5 步与边界情况
