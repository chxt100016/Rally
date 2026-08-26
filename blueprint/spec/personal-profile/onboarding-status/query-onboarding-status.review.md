# personal-profile.onboarding-status.flow.query-onboarding-status 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 首次查询无档案时是否写入记录，返回 NONE 还是新建后的 TBC？
  > 会写入。checkStatus 先观察到 NONE，调用 init 持久化 TBC，但随后固定 return NONE，不重新读取；因此本次响应 NONE、服务终态 TBC，下一次查询才返回 TBC。
  → 已在触发、契约、详细流程、流程图和服务边界明确首次写 TBC 但仍返回读取前 NONE。

- [Q2] 新建 TBC 档案具体初始化哪些字段，哪些值由数据库默认？
  > 领域对象显式设置 userId、status=TBC、videos=[]；仓储插入时生成雪花 bizId。ntrp/utr/更新时间等保持空，数据库默认 reputation/credibility/calibration=0、is_under_review=0、is_newbie=1，并生成时间戳。
  → 已在详细流程、契约和技术线索区分领域显式初始化、仓储生成编号与数据库默认值。

- [Q3] 已有 TBC、NORMAL、UNDER_REVIEW 档案查询时是否发生任何修改？
  > 不修改。只返回从持久化数据转换出的 status，不更新视频、评分、核查标记、时间或任何基础用户字段，也不补齐不完整内容。
  → 已在契约、详细流程和服务边界明确已有三种状态只读返回且不补齐字段。

- [Q4] 两个首次查询并发时如何处理同一用户的重复建档？
  > 没有锁、事务或冲突重试。两个请求可同时读到 NONE 并尝试插入，user_id 唯一键只保留先成功的一条；后到请求以数据库冲突进入系统异常，不改为返回 TBC。
  → 已在详细流程、异常分支和技术线索明确无锁无事务，唯一键保留先成功请求、后到失败。
