# personal-profile.self-rating-update.activity.record-review-trigger 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时写核查触发日志，零所需场次如何处理？
  > 仅 requiredMatches>0 时写；配置为0即使达到涨幅阈值也不会进入日志活动。
  → 已写入触发条件、详细流程第 1 步与边界情况

- [Q2] 触发日志各字段如何填充？
  > type=UNDER_REVIEW，before/after 都是 requiredMatches，reason=USER，固定备注，refId 为空。
  → 已写入活动契约、业务动作 A1-A2 与详细流程第 2 步

- [Q3] 日志与档案更新的事务及大小写缺口是什么？
  > Java 先插触发日志再更新档案但同事务回滚；日志存大写枚举，而后续最新核查日志查询用小写 under_review，可能查不到。
  → 已写入详细流程第 3 步、边界情况与实现提示
