# tournament.booking-reschedule-request.activity.request-rebooking 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 重订分支如何与拒赛分支互斥？
  > confirm=false 且 rebookReason 非空、rejectReason 为空，二者必须恰有一个。
  → 已写入触发条件、异常分支与详细流程第 1 步

- [Q2] 退回 BOOKING 时哪些信息保留或覆盖？
  > 保留订场人、meetupId 和原提交时间；覆盖最近重订请求人、理由、时间。
  → 已写入活动契约、详细流程第 2-3 步与边界情况

- [Q3] 参与者状态和并发如何处理？
  > 全员改 PENDING 并清确认时间，比赛按版本条件与参与关系同事务保存。
  → 已写入业务动作 A4、详细流程第 4-5 步与异常分支
