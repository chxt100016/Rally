# pro-tour-data.upcoming-matches-query.flow.query-upcoming-match-groups 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权、tournamentIds 缺失/空列表及一分钟缓存如何处理？
  > GET /tour/match/upcoming 在普通鉴权排除清单。tournamentIds @RequestParam 必填；完全缺失走全局 OPERATION_FAILED。绑定后的 List 作为 @Cacheable key，元素顺序和内容影响 key，缓存为进程内 Caffeine、写后 1 分钟过期。真正空 List 的内部调用返回 seed=[]/match=[]；缓存命中跳过 DB、翻译和待译登记。
  → 已写入接口契约、详细流程第 1-2/8 步及参数异常分支

- [Q2] 种子范围、淘汰判定、分组与排序规则是什么？
  > 读取所选 tournamentId 下所有 draw 的 entries，不筛 year/drawType/status，保留 seed 非null且非0。淘汰映射来自所选赛事全部大写 FINISHED 比赛，有 winner 时两方中非 winner 记为淘汰，后遍历败局覆盖 round。淘汰→ELIMINATED 组；未淘汰且 tour=ATP→ATP，否则→WTA。组内 seed 升序，组输出按 EnumMap 枚举顺序。
  → 已写入详细流程第 2-3 步与服务边界

- [Q3] 哪些比赛进入待赛结果，球员资料缺失和展示字段如何降级？
  > 先查 tournamentId 命中、matchDate非null、status<>FINISHED 的比赛；收集日期后补同赛事且这些日期上 status=FINISHED 的比赛。双方 ID 都空的丢弃。球员按 playerId 不带 tour 合并；缺资料时比赛中 name=ID/country空，种子条目 name/country可空。状态未知保留原值且标签空；排期、盘分、当前盘、胜方和时长按已有字段降级。
  → 已写入详细流程第 4-5 步及资料缺失降级说明

- [Q4] 日期锚点、球场分组与场内/球场排序规则是什么？
  > 比赛先 scheduledAt nullsLast 排序后按 date 建 LinkedHashMap，最终日期组按 key 升序；base=min(today,最早有效日期)，base/次日显示今天/明天。日期内按原 court（null→空键）分组，同场 winnerId非blank优先，再 scheduledAt。球场双方都有 Court数字则数字升序；有数字组排在无数字后；无数字按场内最小种子，均无种子保持相对不确定次序。计算的 tour 优先未用于排序。
  → 已写入详细流程第 6-7 步及排序技术线索

- [Q5] 翻译覆盖哪些内容，缓存与待译副作用及异常如何收场？
  > 翻译仅球场分组名、比赛双方显示名、种子显示名，zh-CN 命中替换；日期/轮次组名不登记。miss/空译文保留原文并逐条尝试保存，单条失败吞掉。查询/组装/缓存查询未处理异常整体 OPERATION_FAILED，异常前待译不回滚。成功组装后的已翻译 DTO 缓存1分钟，期间数据与新译文变化不反映。
  → 已写入详细流程第 1/8 步、业务活动和翻译异常说明
