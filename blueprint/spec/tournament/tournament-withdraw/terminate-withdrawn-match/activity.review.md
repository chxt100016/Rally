# tournament.tournament-withdraw.activity.terminate-withdrawn-match 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 在途比赛如何选择和处理多场异常数据？
  > 排除 COMPLETED/REJECTED 后无排序取一场；若有多场只处理查到的一场。
  → 已写入触发条件、详细流程第 1 步与边界情况

- [Q2] 终止时是否记录理由或计数？
  > 不记录 rejectReason、不累计拒绝次数，只按版本改 REJECTED。
  → 已写入活动契约与详细流程第 2 步

- [Q3] 本人、他人报名和赛约如何处理？
  > 本人保持 WITHDRAWN，其他仅 IN_MATCH 回 WAITING；仅 DRAFT 赛约关闭，缺失/其他状态不阻止。
  → 已写入详细流程第 3-5 步与边界情况
