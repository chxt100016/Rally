# meetup.meetup-detail.activity.assemble-meetup-recap 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 何时组装 recap，可评价候选有哪些？
  > 实际状态 ONGOING 或 FINISHED 时组装，其他状态 recap=null；不额外要求当前用户参与。waitlistIds 取除当前用户外 JOINED/REVIEWED/SKIPPED 有效参与者。
  → 已写入触发条件、活动契约、业务动作 A1 与详细流程第 1 步

- [Q2] 本人评价如何分组，比分范围与顺序如何处理？
  > 查询 meetupId+fromUserId 的全部 rally_review，按 toUserId 分组并映射各维度；查询该 meetupId 全部 rally_meetup_score，沿仓储现有顺序映射，不追加排序；scoreFilled=比分列表非空。
  → 已写入 reads、业务动作 A2-A3、详细流程第 2-3 步与边界情况

- [Q3] 默认标签配置、空值与读取失败如何处理？
  > 读取 review.default_tags；非空白时按逗号 split 成列表，不 trim、不去重，空白时 defaultTags 保持空。评价、比分或配置读取失败归 SYSTEM_ERROR；没有评价/比分时返回空结构而非取消 recap。
  → 已写入异常分支、业务动作 A4-A5、详细流程第 4-5 步与边界情况
