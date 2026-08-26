# personal-profile.initial-profile-submission.activity.complete-initial-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 哪些档案状态可提交，无档案与重复提交如何处理？
  > NONE 时先建 TBC；TBC/NORMAL/UNDER_REVIEW 都可继续，无首次限制。重复提交完整覆盖档案并重置初始评分。
  → 已写入触发条件、业务动作 A1-A2、详细流程第 1、6 步与边界情况

- [Q2] NTRP、视频、gender、birthday 和资源校验实际是什么？
  > NTRP 只 NotNull，无范围步长；videos 只 NotEmpty，项无级联，逐 key 尝试签名但不校验文件/归属/数量/大小/时长。gender/birthday 完全忽略。
  → 已写入活动契约、业务动作 A3、详细流程第 2-4 步与边界情况

- [Q3] 哪些字段覆盖/重置，核查标记、更新时间与事务如何处理？
  > 覆盖 ntrp/videos/status=NORMAL，重置三项评分；不写 ntrpUpdatedAt，不清 isUnderReview/remainingMatches。保存与返回组装同事务，后者失败整体回滚。
  → 已写入领域依赖、业务动作 A4、详细流程第 3-6 步、边界情况与实现提示
