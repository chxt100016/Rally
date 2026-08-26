# transaction-payment.receipt-recovery.activity.finalize-payment-receipt 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些结论标 PROCESSED 或 FAILED？
  > 无效关联、终态、渠道未付、推进成功标 PROCESSED；订单缺失或渠道/业务异常标 FAILED。
  → 已写入活动契约、异常分支与详细流程第 1-2 步

- [Q2] 失败留痕更新本身失败如何处理？
  > 可能中断当前扫描，剩余 RECEIVED 留到下轮。
  → 已写入异常分支、详细流程第 3-4 步

- [Q3] 日志终态是否证明业务完整？
  > 不证明；PROCESSED/FAILED 都不会再被 RECEIVED 扫描，可能保留业务缺口。
  → 已写入边界情况与实现提示
