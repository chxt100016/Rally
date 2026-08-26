# personal-profile.player-home.flow.query-player-home 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 球员主页公开哪些基础字段，是否受关注关系或隐私开关限制，城市名称是否返回？
  > 公开 userId、nickname、签名头像、gender、birthday、cityCode、bio；不检查关注关系或隐私开关，登录即可查看本人或他人。MyProfileUserDTO 的 cityName 字段在此构建器中未赋值，始终为 null。
  → 已在触发、契约、详细流程和技术线索明确公开字段、无关注隐私限制及 cityName 不赋值。

- [Q2] 目标球员无网球档案、TBC 档案或评分字段缺失时等级、评级和视频如何返回？
  > profile=null 时 level 对象字段空、score.profileLevel 为空字符串、video total=0/list=[]。只要 profile 非 null 就直接计算评分；TBC 常见三项评分若为 null 会拆箱空指针，整页失败。videos=null 则视频空列表，但 NTRP 等字段可为空原样返回。
  → 已在详细流程、契约和异常说明明确无档案成功降级、TBC 或缺评分档案可能整页失败。

- [Q3] 最近约球的纳入条件、排序和数量上限是什么，发布者是否需要有效报名？
  > 查询所有 status!=DRAFT 且目标用户为 creator，或存在 JOINED/REVIEWED/SKIPPED 报名的约球；creator 分支不要求任何报名。按 meetup.biz_id 降序，SQL limit 4 用第 4 条探测 hasMore，PageDTO 实际列表最多 3 条。
  → 已在详细流程、契约和技术线索明确非草稿、发布者绕过报名、三种报名状态、bizId 排序及最多三条。

- [Q4] 获评 total、标签以及比分 total、类型数、最近比分分别采用什么统计口径？
  > 获评 total=LEVEL_VOTE 记录数+ATTENDANCE_VOTE 记录数+TAG 拆分次数，top 标签最多5；主页不赋三个明细计数字段。比分 total 按所有盘含 RALLY，single/double 分别计数但无 rallyCount，按 bizId 降序取10盘明细。
  → 已在详细流程、契约和技术线索明确获评及比分统计粒度、RALLY 计入总数和最近十盘。

- [Q5] 任一关注、约球、评价、档案、比分或资源子查询失败时是否允许部分返回？
  > 不允许部分返回。DTO 采用连续同步子构建，任何用户、关注、约球/球场卡片、评价、配置、档案评分、视频封面、比分转换或七牛签名异常都会让整个接口失败。
  → 已在触发、流程图、异常分支和服务边界明确串行聚合无部分成功。
