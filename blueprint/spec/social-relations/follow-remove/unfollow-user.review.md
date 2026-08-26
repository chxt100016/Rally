# social-relations.follow-remove.flow.unfollow-user 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 登录与 targetUserId 请求校验规则是什么？
  > POST /user/follow/cancel 受普通登录鉴权，UserContext 提供当前 followerId。请求体缺失/非法走全局失败；FollowCmd.targetUserId 用 @NotBlank，空白报“targetUserId: 目标用户不能为空”，非空不 trim/格式校验。
  → 已写入接口契约、详细流程第 1 步及鉴权/参数异常分支

- [Q2] 目标不存在、目标为本人或原关系不存在时如何处理？
  > unfollow 不调用用户资料服务，不校验 target 是否存在，也不比较 target 与当前用户。无论关系原本不存在、目标不存在或为本人，都按同一 follower+following 删除，影响0行仍成功。
  → 已写入详细流程第 2-3 步、服务边界与幂等分支

- [Q3] 删除条件、事务结果和成功响应是什么？
  > repository 以 follower_id=当前用户 且 following_id=targetUserId 执行单条条件 remove，不先 exists；无显式业务事务、无反向删除。异常走 OPERATION_FAILED；正常返回 Result.ok() data=null，不返回影响行数。
  → 已写入详细流程第 3-4 步、业务活动及删除异常分支
