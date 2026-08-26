# pro-tour-data.player-ranking-collect.activity.collect-wta-player-rankings 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] WTA 何时执行，ATP 失败或空来源时有何差异？
  > ATP成功提交或空来源跳过后执行；ATP异常则不执行。
  → 已写入触发条件与活动契约

- [Q2] WTA 的转换、身份和覆盖规则是什么？
  > 强制tour=WTA，按(tour,playerId)过滤去重；新身份插入，存量仅由非空来源字段覆盖，未出现旧球员保留。
  → 已写入业务动作 A1-A3 与详细流程第 1-3 步

- [Q3] WTA 失败是否回滚 ATP，最终响应是什么？
  > WTA独立事务，失败仅回滚自身，已提交ATP保留；两路结束手动返回排名采集完成，定时静默。
  → 已写入异常分支、详细流程第 4-5 步与边界情况
