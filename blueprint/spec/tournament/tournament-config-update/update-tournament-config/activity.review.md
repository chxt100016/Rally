# tournament.tournament-config-update.activity.update-tournament-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些赛事状态允许更新？
  > DRAFT、ACTIVE、ABANDONED 均允许，不设状态限制。
  → 已写入触发条件、活动契约与边界情况

- [Q2] 可选字段传 null 是否清除旧值？
  > 不能；映射后实体更新忽略 null，数据库旧值保留。
  → 已写入活动契约、详细流程第 3 步与边界情况

- [Q3] 城市和运营进度如何处理？
  > cityCode 可变但 cityName 不重算；状态、轮次、锁位、结束时间和线下关联保留。
  → 已写入详细流程第 4-5 步与边界情况

- [Q4] 配置更新是否继续不限制赛事状态，使 DRAFT、ACTIVE、FINISHED、ABANDONED 都可修改配置？
  > 是。继续不做状态门禁，四种现有赛事状态均可更新配置；不把 DRAFT 限制补进实现。
  → 已落实到触发条件、活动契约、业务动作 A1、详细流程第 1 步与边界情况。

- [Q5] 除 offlineFromRound 显式允许写 null 外，其他可选字段传 null 是否继续受实体非空更新策略影响而保留数据库旧值？
  > 是。仅 offlineFromRound 通过显式列更新支持清空；其他可选字段的 null 继续被非空更新策略忽略并保留旧值。
  → 已落实到活动契约、业务动作 A3、详细流程第 3 步与边界情况。

- [Q6] cityCode 更新时是否继续不重新查询或同步 cityName，并保持所有运营进度字段及关联对象不联动？
  > 是。cityCode 变化不刷新 cityName；运营进度字段全部保留，也不联动报名、比赛、支付或匹配。
  → 已落实到业务动作 A2/A4、详细流程第 4/5 步与边界情况。
