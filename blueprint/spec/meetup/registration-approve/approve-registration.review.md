# meetup.registration-approve.flow.approve-registration 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 审批通过是否检查剩余名额、加入方式、报名到期与申请人当前准入条件？
  > 按 Java 实现确认：approve 只找指定报名、校验发布者、PENDING 和 meetup.isActive；不检查 isFull、joinMode、expiresAt，也不读取申请人资料或重做性别、NTRP、信誉、时间冲突准入。
  → 已落入详细流程、异常分支和服务边界。

- [Q2] 哪些约球状态可审批，草稿和进行中的约球是否允许？
  > 按 Java 实现确认：isActive 使用 realStatus != FINISHED && != CLOSED，因此 DRAFT、OPEN、ONGOING 均允许；OPEN 已过时间会懒判为 ONGOING 或 FINISHED，ONGOING 仍允许。
  → 已落入详细流程和技术线索。

- [Q3] 申请人已存在群聊成员时，报名和当前人数如何收场，历史消息如何计入未读？
  > 按 Java 实现确认：报名与 currentPlayers 先在事务中保存，再 ChatDomainService.join；已有成员报 ALREADY_JOINED_CHAT，事务回滚前述变更。新聊天成员 unreadCount=0、lastReadMessageId=null，审批前历史消息不会计入初始未读。
  → 已落入详细流程、流程图和异常分支。

- [Q4] 审批后如何选择通知，重复触达和渠道失败如何处理？
  > 审批后 isFull（>=上限）时只触发 TEAM_SUCCESS 给全体有效参与者，否则触发 JOIN_SUCCESS 给申请人。事件使用本次报名编号并在提交后异步发送；同一事件重复触发被日志唯一键跳过，未订阅或渠道失败不回滚审批。
  → 已落入详细流程、流程图和异常分支后的通知说明。

- [Q5] 审批通过是否继续不检查剩余名额与并发占位？
  > 是。保持 main 逻辑，不增加名额或版本条件。
  → 已落入详细流程第 3、4 步与异常分支说明。

- [Q6] DRAFT 与 ONGOING 约球是否继续允许审批 PENDING 报名？
  > 是。只拒绝实际已结束或已关闭，继续允许 DRAFT、OPEN、ONGOING。
  → 已落入详细流程第 2、3 步与技术线索。

- [Q7] 审批后人数达到或超过上限时是否继续发送组团成功，否则发送报名成功？
  > 是。达到或超过上限发组团成功，否则向申请人发报名成功，通知失败不回滚。
  → 已落入业务活动、流程图与详细流程第 6、7 步。
