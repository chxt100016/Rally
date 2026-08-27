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

- [Q4] totalSlots 是否继续仅允许 2 到 64 的 2 次方，且 totalSlots=2 仍视为合法？
  > 是。totalSlots 继续限制为 2–64 且为 2 的整数次幂，最小值 2 合法。
  → 已落实到活动契约、业务动作 A1、详细流程第 2 步与边界情况。

- [Q5] offlineFromRound 为空是否继续表示全程线上，非空时只要求对应签位小于 totalSlots，不新增线下活动创建？
  > 是。空值表示全程线上；非空只做轮次签位关系校验，创建草稿不联动生成线下活动。
  → 已落实到活动契约、业务动作 A1、详细流程第 2/6 步与边界情况。

- [Q6] cityCode 查不到城市时是否继续由现有未处理异常统一收敛为 OPERATION_FAILED，而不新增城市专用错误码？
  > 是。城市缺失继续由现有异常路径收敛为 OPERATION_FAILED，不改变错误码接口。
  → 已落实到异常分支、@system.location-catalog、业务动作 A2、详细流程第 4 步与边界情况。
