# meetup.meetup-join.flow.join-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 草稿约球及缺失性别、NTRP 或信誉分的完整档案用户，报名时如何处理？
  > 按 Java 实现确认：assertCanJoin 拒绝 CLOSED、FINISHED、ONGOING 和开始时间已过，但不拒绝 DRAFT。通过 assertCompleted 后，准入检查仍把 user.gender=null、profile.ntrpScore=null、profile.reputationScore=null 分别视为符合；UNDISCLOSED 在性别有限制时不符合。
  → 已落入详细流程和异常分支后的准入说明。

- [Q2] 自动撤回时间和分享用户编号是否校验、归因或触发后续动作？
  > 按 Java 实现确认：autoWithdrawAt 原样写入 expiresAt，不校验是否过去、是否晚于开始或是否仅用于 PENDING，代码中没有按该时间调度撤回。shareUserId 只在非 null 时记日志，不保存、不归因、不影响结果。
  → 已落入请求参数、详细流程和服务边界。

- [Q3] 新报名业务编号和接口返回的 JOINED 或 PENDING 终态，实际是否提供？
  > 按 Java 实现确认：RegistrationData 新建时不设置 bizId，聚合保存路径也不补业务编号，仅数据库自增 id；接口返回 Result<Void>，不会返回报名编号或 JOINED/PENDING，只有应用内部用状态决定后续分支。
  → 已落入成功响应和详细流程。

- [Q4] 订阅授权场景如何过滤，授权登记或通知失败是否影响报名事务？
  > 按 Java 实现确认：parseScenes 对所有 NoticeScene 名称开放，非法名称忽略，重复不去重。直接加入并满员时只过滤 JOIN_SUCCESS。grant 内部吞掉异常；notify 注册提交后异步任务并吞掉触发/发送异常，二者均不回滚报名。
  → 已落入请求参数、详细流程和异常分支后的通知说明。

- [Q5] 补充检查 RegistrationData 构造器后，用户报名是否实际生成业务报名编号？
  > 更正先前 Q3 的部分结论：RegistrationData 无参构造器会通过 IdWorker 生成 bizId，因此直接加入和待审批报名都具有业务报名编号并按该编号保存；接口仍不返回报名编号或 JOINED/PENDING 终态。
  → 已更正详细流程，明确构造器生成业务报名编号；接口仍不返回编号与终态。
