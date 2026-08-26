# social-relations.follow-create.flow.follow-user 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 登录、请求体和 targetUserId 的校验及错误是什么？
  > POST /user/follow 受普通 AuthInterceptor 保护，从 UserContext 取 followerId。无/非法 token 分别返回登录过期或凭证无效。JSON 缺失/非法走 OPERATION_FAILED；targetUserId 用 @NotBlank，空白提示“targetUserId: 目标用户不能为空”，非空不 trim/不校验格式长度。
  → 已写入接口契约、详细流程第 1 步及鉴权/参数异常分支

- [Q2] 自关注、目标不存在与重复关注如何处理？
  > followerId.equals(targetUserId) 以 FOLLOW_SELF_NOT_ALLOWED 拒绝并提示不能关注自己。随后 UserProfileDomainService.get(target) 要求用户聚合中的 user 存在，缺失走复用的 TOKEN_INVALID。已存在 follower+following 关系直接 return，原 bizId/createTime 不变。
  → 已写入详细流程第 2-4 步及本人、目标与幂等分支

- [Q3] 首次关注如何保存，并发、事务与成功响应规则是什么？
  > 不存在时生成雪花 bizId 并 save UserFollowPO，数据库唯一键保证同向唯一。exists 与 insert 没有一个外层事务/锁，并发首次关注可能一条成功、另一条唯一冲突转 OPERATION_FAILED；成功未检查 save boolean。返回 Result.ok() 的 data=null，不返回关系或目标信息。
  → 已写入详细流程第 5-6 步、业务活动及并发异常分支
