# meetup.meetup-edit.flow.edit-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 非发布者编辑时，发布者权限校验与赛事阶段、锁定点、城市和成员锁定校验的实际先后顺序是什么？
  > 按 Java 实现确认：MeetupPolicy.assertEdit 依次校验赛事比赛阶段、实际状态与编辑锁定点、城市不变、已有参与者后的时间地点锁定；该方法把创建者编号传给 canEdit，因此不核对当前用户。真正的 meetup.assertOwner 在随后保存活动中执行，所以非发布者可能先收到前置规则错误。
  → 已落入详细流程、流程图和异常分支。

- [Q2] 编辑是否复用发布时的开始时间、持续时长和 NTRP 组合校验？
  > 按 Java 实现确认：不复用 assertPublish。Bean Validation 只约束必填、长度、人数范围、经纬度和 NTRP 单值范围；不拒绝过去开始时间、任意持续时长、模式所需边界缺失、上下界关系或非 0.5 步长。
  → 已落入请求参数和详细流程。

- [Q3] 费用明细为空值或空列表，以及通知授权和区县编码字段，实际如何保存？
  > 按 Java 生成映射确认：costItems 为 null 时保留原列表，空列表时清空；acceptedNoticeScenes 不进入映射也不创建通知额度；districtCode 不保存，districtName 由命中球场库的区县名或最终地址解析。hourlyAllocations 不在编辑命令中，原按人时资料随现有 CostData 结构保留。
  → 已落入请求参数、详细流程和服务边界。

- [Q4] 文字或地图模式命中球场库后，哪些约球身份和审计字段会被球场数据覆盖，保存结果是什么？
  > 按 Java 生成的 MapStruct 实现确认：命中 courtData 时除场地名称、地址、坐标、区县外，还会覆盖 meetup.bizId、cityName、createTime、updateTime，并把 cmd.courtId 写入 courtId。Repository 随后按被覆盖的 bizId 保存，可能新增或覆盖以球场编号为主键的记录，而原约球保持不变；事务没有专门防护。
  → 已落入详细流程、异常分支和技术线索。
