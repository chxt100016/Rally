# personal-profile.initial-profile-submission.flow.submit-initial-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 初始档案是否只能提交一次，NORMAL 或 UNDER_REVIEW 重复提交会改哪些字段？
  > 不是。NONE、TBC、NORMAL、UNDER_REVIEW 都可提交。每次都会用请求 NTRP 和完整 videos 覆盖，把 status 设 NORMAL，并将 reputation、credibility、calibration 重置为当前三项初始配置；旧视频整体替换。
  → 已在触发、详细流程、异常说明和服务边界明确所有状态可重复提交，并覆盖 NTRP、视频、状态和三项初始评分。

- [Q2] 从核查期重复提交时是否清除核查标记、剩余场次和 NTRP 冷却时间？
  > 不清除 isUnderReview 和 reviewRemainingMatches，也不改 ntrpUpdatedAt。UNDER_REVIEW 重提后可出现 status=NORMAL 但 isUnderReview=true、剩余场次仍在；既有冷却起点保持原值，首次没有则仍没有。
  → 已在详细流程、异常说明和技术线索明确核查标记、剩余场次与 NTRP 更新时间均不清理。

- [Q3] gender 与 birthday 是否保存，NTRP 有哪些数值校验？
  > gender 和 birthday 虽在命令中但业务代码完全不读取、不保存。ntrpScore 只校验非 null，不校验 1.5 到 7.0、0.5 步长、正数或精度；最终受数据库 DECIMAL(3,1) 存储能力约束。
  → 已在契约、详细流程和技术线索明确性别生日被忽略，NTRP 仅非空且受数据库存储约束。

- [Q4] 视频项、资源标识、数量、大小、时长和归属采用哪些校验？
  > 只校验 videos 列表非 null 且非空。列表项无嵌套校验，null 项会在遍历时异常；空白 key 可通过且返回地址为空。没有数量、大小、时长、标题、扩展名、文件存在和资源归属校验；无扩展名的非空 key 在返回封面时失败。
  → 已在契约、详细流程、异常分支和服务边界明确只校验列表非空，视频项与资源没有业务有效性限制。

- [Q5] 新建或覆盖档案后，返回聚合失败是否回滚全部修改？
  > 会。submit 标注 @Transactional，NONE 时创建 TBC、完成覆盖、保存以及 getMyProfile 聚合都在同一事务；任一运行时异常都会回滚新建或更新。
  → 已在详细流程、流程图和异常分支明确初始化、覆盖与聚合同事务，返回失败全部回滚。
