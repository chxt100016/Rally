# meetup.my-meetups.flow.list-my-meetups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 待处理阶段三类结果如何合并，同一约球命中多个原因时是否去重及哪个标签优先？
  > 按 Java SQL 确认：PENDING 用 UNION 合并 PENDING_APPROVAL、PENDING_REVIEW、UNREAD_MESSAGES。因为 _pending_reason 不同，同一 meetup 的行不相同，UNION 不会跨原因去重，会重复交付；每行直接使用自身 reason 标签，没有优先级。PENDING_REVIEW 只要求本人报名 JOINED，不检查比分或评价。
  → 已落入详细流程、成功响应和异常分支后的重复说明。

- [Q2] 进行中、我发布、已完成和最近四个阶段的实际状态、报名和时间条件是什么？
  > 按 Java 实现确认：IN_PROGRESS 要本人创建或有效报名、meetup.status=OPEN、end_time>NOW；MY_PUBLISH 要 creatorId=本人且非 DRAFT；COMPLETED 要本人报名 REVIEWED/SKIPPED 且约球非 DRAFT，不看时间状态；RECENT 要本人创建或有效报名且非 DRAFT，无时间窗口。
  → 已落入详细流程和业务活动。

- [Q3] 续页标识和页大小为空或异常时如何处理，是否有单页上限和总量？
  > 按 Java 实现确认：lastId 空白/非法编码按首页；解码数组首项强转 String，非字符串会系统异常。size 默认10、只校验>=1，显式 null 在 size+1 拆箱时系统异常，无最大值。所有 tab 多查一条，total 固定 null。
  → 已落入请求参数、成功响应和异常分支。

- [Q4] 不同参与阶段的卡片主标签如何计算，是否会按当前时间修正存储状态？
  > 按 Java 实现确认：PENDING 用 pendingReason 标签；IN_PROGRESS、COMPLETED 用 districtName；MY_PUBLISH、RECENT 若存储状态 OPEN 且 endTime 已过则显示 FINISHED 文案，否则用存储状态 label。卡片 status 字段本身仍是存储状态，不被改写。
  → 已落入详细流程、成功响应和技术线索。
