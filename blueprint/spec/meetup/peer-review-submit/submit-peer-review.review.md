# meetup.peer-review-submit.flow.submit-peer-review 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些报名身份、约球阶段和评价截止时间允许提交评价？
  > 按 Java 实现确认：Meetup.assertIn 接受创建者以及 PENDING、JOINED、REVIEWED、SKIPPED；实际状态必须 ONGOING 或 FINISHED。截止为 endTime 加 review.deadline_days，只有 now.isAfter(deadlineAt) 才拒绝，因此恰好等于截止时刻仍允许。
  → 已落入触发、详细流程和异常分支。

- [Q2] 被评价人是否必须是本场其他有效参与者，是否禁止自评或校验账户存在？
  > 按 Java 实现确认：submitReview 不调用被评价人资格错误定义，不禁止 toUserId=本人，不查询账户，也不要求目标在约球报名中；这些目标的评价照常保存，但覆盖完成只比较其他有效参与者。
  → 已落入请求参数、详细流程和异常分支后的目标说明。

- [Q3] 评价列表为空、缺失、含重复维度或标签为空白、自定义时如何处理？
  > 按 Java 实现确认：reviews 没有 NotNull，null 或列表中 null 项会系统异常；空列表不写评价但仍检查既有覆盖。重复同一维度逐项 upsert，最后一项生效。TAG 只要求 value!=null，允许空串、自定义、逗号文本，未校验默认标签、数量或长度。
  → 已落入请求参数、详细流程和异常分支。

- [Q4] 评价覆盖完成后哪些报名状态会推进为 REVIEWED，PENDING、REVIEWED、SKIPPED 如何处理？
  > 按 Java 实现确认：REVIEWED/SKIPPED 在保存评价后由 meetup.hasReview 短路，状态不变。其他身份完成覆盖时调用 toReviewed，但 SQL 只更新当前 JOINED，所以 JOINED 转 REVIEWED 并写 optTime，PENDING 保持不变；创建者报名通常为 JOINED。
  → 已落入详细流程、成功响应和服务边界。
