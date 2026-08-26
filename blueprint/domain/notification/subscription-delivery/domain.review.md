# @notification.subscription-delivery 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 该模型应继续是无状态领域服务，还是拥有 user_notify_subscribe 的聚合？
  > 改为聚合。以用户+业务类型+关联业务+场景的额度池为边界，每条订阅授权流水是实体；微信渠道发送在聚合外。
  → 类型、聚合清单、边界与状态

- [Q2] 一次请求内重复授权场景和多次真实授权分别如何登记，是否需要场景白名单？
  > 一次请求内重复场景先去重；不同请求代表不同次真实授权，可各建一条额度。业务类型与场景必须通过领域白名单，MEETUP 不能登记赛事场景，反之亦然。
  → I2、C1 与授权重复边界情况

- [Q3] 发送前资格复核异常时应 fail-open 消费额度并发送，还是 fail-closed 保留额度？
  > fail-closed。只有调用方明确给出 ELIGIBLE 才可占用；INELIGIBLE 或 UNKNOWN 都跳过并保留 UNUSED，避免向已退出或无资格用户误发。
  → I4、C2 与资格异常边界情况

- [Q4] 额度状态如何流转，发送失败或过期额度能否再次消费？
  > UNUSED 通过 CAS 进入 SENDING，再终结为 SENT 或 FAILED；过期进入 EXPIRED。SENT/FAILED/EXPIRED 都是终态不重用，发送失败需新的授权额度。
  → 状态、I3/I5、C2-C4 与失败边界情况
