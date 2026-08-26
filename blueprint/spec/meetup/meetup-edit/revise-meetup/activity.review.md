# meetup.meetup-edit.activity.revise-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 编辑校验的执行顺序、赛事比赛状态和权限时机是什么？
  > 先加载约球；若 TOURNAMENT，按 meetupId 找关联 match，仅 BOOKING/SCHEDULED 可编辑。再用配置锁定分钟、实际状态判断 canEdit（实现传 creatorId，等价仅状态/时间），再校验城市与已有参与者时的时间地点不变，最后在真正 edit 时才 assertOwner。因此非创建者可能先收到赛事/状态/城市/地点错误。
  → 已写入异常分支、领域依赖、业务动作 A1-A3 与详细流程第 1-4 步

- [Q2] 哪些字段变化受参与者锁定，场地库如何降级？
  > 已批准参与人数 countApproved>1 时，startTime、duration、courtName、courtAddress、courtLng/Lat 任一变化禁止；courtId/courtSelectMode 本身不在比较。TEXT/MAP 且 courtId 非空时查 @court.court，命中用名称地址坐标区县，未命中降级请求值并从地址提区县；FREE 不查。城市必须保持原 cityCode。
  → 已写入领域依赖、业务动作 A2-A3、详细流程第 3、5 步与边界情况

- [Q3] MapStruct 整体更新的空值、费用与已知字段覆盖缺陷如何记录？
  > nullValue IGNORE 通常保留 null 入参字段；costItems null 保留、空列表清空；districtCode/acceptedNoticeScenes 不保存，hourlyAllocations 不在命令。重算 endTime 依赖 startTime+duration。命中 courtData 时生成映射会额外把 meetup.bizId、cityName、createTime、updateTime 覆盖为球场值，随后 save 可能按被覆盖 bizId 更新错误目标；这是当前真实实现，报/不报取决于仓储，不能在文档中修正。
  → 已写入业务动作 A4-A5、详细流程第 6-9 步、边界情况与实现提示
