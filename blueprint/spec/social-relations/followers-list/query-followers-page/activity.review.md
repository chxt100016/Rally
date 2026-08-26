# social-relations.followers-list.activity.query-followers-page 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 名单所属用户和游标如何解释？
  > userId 非空白原样使用，否则当前用户；lastId 原样参与 bizId 小于比较，关系倒序取 size+1。
  → 已写入业务动作 A1-A2、详细流程第 1-2 步与边界情况

- [Q2] 分页元数据与资料缺失如何处理？
  > 最多返回 size 条并用多取一条判 hasMore；total/nextCursor=null，资料缺失仍保留 userId/cursor。
  → 已写入活动契约与详细流程第 3-4 步

- [Q3] 粉丝页 isFollowed 表示谁的关系？
  > 表示当前登录用户是否关注本页粉丝，不表示名单所属用户是否回关。
  → 已写入业务动作 A4 与详细流程第 5 步
