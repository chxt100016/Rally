# social-relations.following-list.flow.query-following 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 登录、userId、lastId、size 的默认与校验规则是什么？
  > GET /user/follow/following 受普通登录鉴权。userId/lastId 可选，size 默认20、最小1无上限，非整数报参数类型错误。userId 非blank原样使用，否则当前用户；不验证目标存在。lastId 非blank原样比较。
  → 已写入接口契约、详细流程第 1-2 步及鉴权/参数分支

- [Q2] 关注关系如何查询、排序、判断 hasMore，分页字段如何返回？
  > 按 follower_id=目标查询，lastId 非blank时 biz_id < lastId，biz_id DESC，LIMIT size+1。rows>size 裁为 size、hasMore=true。total/nextCursor 始终 null；每项 cursor=关系 bizId，客户端取末项继续。
  → 已写入详细流程第 2-3 步、接口契约和查询技术线索

- [Q3] 资料缺失、头像签名与 isFollowed 的精确含义和失败规则是什么？
  > 以 followingId 补 UserProfile；缺资料仍保留关系用户编号与 cursor，nickname/avatar/NTRP空。头像签名3600秒。isFollowed 总是当前登录用户是否关注该 followingId；查看本人列表通常为真，查看他人不代表回关。读取/签名异常整体失败。
  → 已写入详细流程第 4-5 步及资料缺失/异常说明
