# personal-profile.my-profile.flow.query-my-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 不同档案状态如何决定是否查询并返回统计、等级、评分和视频？
  > 顶层 status 来自 profile.status，无档案为 NONE。hasProfile 只在 status 不是 NONE/TBC 时为真；NONE、TBC 只构建 user，stats/level/score/video 为 null；NORMAL、UNDER_REVIEW 才构建全部分组。
  → 已在契约、详细流程、流程图和技术线索明确 NONE/TBC 只返回基础分组，NORMAL/UNDER_REVIEW 返回全部分组。

- [Q2] 已完成约球数量采用什么口径，是否检查约球结束、比分或时间？
  > 只按当前用户 registration.status 属于 REVIEWED 或 SKIPPED 统计报名/约球行，不判断 meetup.status、开始结束时间、比分是否存在或评价内容；以查询 SQL 的完成态关系为准。
  → 已在详细流程和技术线索明确完成数只按 REVIEWED/SKIPPED 报名统计，不看约球状态、时间或比分。

- [Q3] NTRP 提示和可修改状态依据哪些字段，status 与 isUnderReview 冲突如何处理？
  > 冷却依据 ntrpUpdatedAt 与可信度分档配置；核查剩余场次依据 isUnderReview，而非顶层 status。提示优先级为冷却高于核查高于系统建议；canModify 要求冷却与剩余场次都为空或 0。status 和 isUnderReview 冲突时原样反映，不修正。
  → 已在详细流程和技术线索明确冷却、核查的字段来源、提示优先级及冲突不修正。

- [Q4] 系统建议、冻结说明和综合评级是否来自真实战绩及哪些配置？
  > 系统建议及真实水平 4.5 文案固定，未读近 20 场；noticeInfo 固定写 90 天。实际冷却天数按 credibility 小于 30、小于 60、其余三档读取配置。综合评级由三项评分与 S/A/B 阈值配置计算。
  → 已在详细流程和技术线索明确固定建议与 90 天文案，以及实际冷却和评级配置来源。

- [Q5] 未知城市、缺失档案字段、异常视频 key 或数字配置错误时如何返回？
  > 非空未知城市、正常档案缺失 NTRP 或 videos、状态/视频 JSON 无法转换、非空视频 key 无扩展名、七牛构址或依赖查询失败会终止整份查询。数字配置解析失败按 0 降级继续；空城市、空头像和空视频 key 返回空地址。
  → 已在异常分支和技术线索明确整份查询失败项、空值降级项及数字配置按 0 降级。
