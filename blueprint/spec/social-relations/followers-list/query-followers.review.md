# social-relations.followers-list.flow.query-followers 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 登录、userId、lastId、size 的默认与校验规则是什么？
  > GET /user/follow/followers 受普通登录鉴权。FollowListCmd.userId/lastId 可选，size 默认20、@Min(1)、无最大值，非整数报参数类型错误。userId StringUtils.isNotBlank 时原样使用，否则当前 UserContext；不验证目标存在。lastId 非blank原样比较。
  → 已写入接口契约、详细流程第 1-2 步及鉴权/参数分支

- [Q2] 粉丝关系如何查询、排序、判断 hasMore，分页字段如何返回？
  > 按 following_id=目标查询，lastId 非blank时 biz_id < lastId，biz_id DESC，LIMIT size+1。rows>size 时裁到 size 且 hasMore=true，否则 false。PageDTO.total=null、nextCursor=null；每项 cursor=关系 bizId，末项 cursor 由客户端作为下页 lastId。
  → 已写入详细流程第 2-3 步、接口契约和查询技术线索

- [Q3] 资料缺失、头像签名与 isFollowed 的精确含义和失败规则是什么？
  > 以 followerId 批量查 UserProfile；缺聚合仍保留 userId/cursor/isFollowed，展示字段空。头像键直接 buildSignedUrl，默认3600秒；当前用户对本页 followerIds 的关系集合决定 isFollowed，不代表被查看用户回关。关系/资料/签名异常整体 OPERATION_FAILED，不返回部分页。
  → 已写入详细流程第 4-5 步及资料缺失/异常说明
