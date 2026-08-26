# tournament.tournament-draft-create.activity.create-tournament-draft 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 签位、线下轮次和数值核心规则是什么？
  > 总签位为 2–64 的 2 次方，线下轮次签位小于总签位，费用/上限非负且资格组至少 2 人。
  → 已写入业务动作 A1 与详细流程第 2 步

- [Q2] 城市编码缺失如何报错？
  > 按 cityCode 查 cityName，无结果当前以未处理异常收敛为 OPERATION_FAILED。
  → 已写入异常分支、详细流程第 4 步与边界情况

- [Q3] 新草稿初始值是什么？
  > status=DRAFT、currentRound=QUALIFIER、currentFilledSlots=0，endTime/offlineMeetupId 为空。
  → 已写入活动契约与详细流程第 5-6 步
