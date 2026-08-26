# transaction-payment.payment-result-receipt.activity.advance-paid-business 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 报名付款如何占位和推进？
  > 原子锁位+1，要求报名 PAYING，再转 MAIN/WAITING 和总签位对应首轮。
  → 已写入活动契约与详细流程第 1-3 步

- [Q2] 何时推进赛事并淘汰剩余资格报名？
  > 资格赛比赛完成且锁位已满时进入正赛首轮，剩余 QUALIFY/WAITING 变 ELIMINATED。
  → 已写入业务动作 A4 与详细流程第 4 步

- [Q3] 回调路径失败能否保证回滚？
  > 不能；异常内部捕获，订单 PAID、席位等前序变化可能提交，回执 FAILED。
  → 已写入异常分支、详细流程第 5 步与边界情况
