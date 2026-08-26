# meetup.meetup-cost-update.activity.replace-meetup-cost 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 操作权限、约球类型状态和时间限制是什么？
  > 只校验当前 userId 为创建者；约球不存在 MEETUP_NOT_FOUND、非创建者 NOT_CREATOR。不限制 NORMAL/TOURNAMENT、实际状态、开始前后，也不通知成员。
  → 已写入触发条件、异常分支、业务动作 A1 与详细流程第 1 步

- [Q2] 分摊段时长、总时长、参与者与重复覆盖规则如何校验？
  > hourlyAllocations 非空时总 duration 必须精确 compareTo 约球 duration；每段 duration>0、userIds 非空，每个用户必须是创建者或 JOINED/REVIEWED/SKIPPED。不限段数、精度、同段/跨段重复，不要求覆盖全部参与者。null duration 在求和前触发异常，当前归 SYSTEM_ERROR。
  → 已写入活动契约、业务动作 A2、详细流程第 2-4 步与边界情况

- [Q3] 费用项与空列表/null 如何保存，事务及并发语义是什么？
  > costItems 不校验名称、金额、负数、重复。每次新建 CostData 并把请求 costItems/hourlyAllocations 原样整体赋值；null 或空列表都会替换原值（null 保存为 JSON 中相应空值/字段语义依映射）。应用事务内保存，失败回滚；无版本控制，并发后写覆盖。
  → 已写入活动契约、业务动作 A3-A4、详细流程第 5-7 步与边界情况
