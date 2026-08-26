# @tournament.tournament 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 赛事生命周期是否需要补充 COMPLETED 状态？
  > 本轮不补。现有持久化和活动只支持 DRAFT→ACTIVE→ABANDONED；决赛完成仍停在 ACTIVE/FINAL，endTime 与赛事收官尚无明确命令。
  → 轮次服务已确认不负责赛事收官，不能在赛事聚合中凭空增加无调用方的状态。

- [Q2] 运营配置更新能否覆盖运行进度字段？
  > 不能。配置更新可在任意生命周期状态执行，但只改配置字段；status、currentRound、currentFilledSlots、endTime、offlineMeetupId 必须保留。
  → 配置更新活动明确划分可配字段与运营进度，二者需要不同命令。

- [Q3] offlineFromRound 接受哪些值？
  > 沿用当前可执行枚举与大小规则：接受 QUALIFIER 及正赛轮次，但该轮 slotCount 必须严格小于 totalSlots；不按旧表注释缩窄为仅 ROUND_4/8/16。
  → 创建策略直接使用完整轮次枚举，且 totalSlots=2 已被活动确认为合法，按现状可避免契约自相矛盾。

- [Q4] 席位锁定和轮次推进怎样防止并发超卖或回退？
  > 席位以 currentFilledSlots<totalSlots 条件原子加一；轮次只接受顺序更晚的条件更新，重复或较旧建议为空操作。
  → 四条支付路径依赖条件锁位，轮次服务也要求赛事聚合执行单向推进。

- [Q5] 配置更新改变 cityCode 时 cityName 如何处理？
  > 必须重新通过 location-catalog 解析并把 cityCode/cityName 成对替换；禁止只改编码保留旧名称。
  → 现有活动已记录编码名称可能不一致的缺陷，领域不变量应明确消除该漂移。
