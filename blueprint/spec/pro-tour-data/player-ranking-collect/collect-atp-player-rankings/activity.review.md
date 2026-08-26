# pro-tour-data.player-ranking-collect.activity.collect-atp-player-rankings 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] ATP 来源为空与来源异常分别如何推进？
  > 响应/排名节点/数组为空时不改ATP并继续WTA；来源抛异常时终止流程且不处理WTA。
  → 已写入活动契约、异常分支与详细流程第 1 步

- [Q2] 记录转换、过滤和出生日期口径是什么？
  > 强制tour=ATP，转playerId/姓名/国家/rank/points/birthDate；日期空过短或ISO失败为null，playerId或tour空记录丢弃，按复合身份批内去重。
  → 已写入业务动作 A2 与详细流程第 2-3 步

- [Q3] 存量覆盖、未出现球员和事务如何处理？
  > 新增未收录；已有只被来源非空字段覆盖；未出现存量不删不清排名。ATP独立事务，失败回滚且阻断WTA。
  → 已写入业务动作 A3、详细流程第 4-5 步与边界情况
