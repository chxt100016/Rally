# pro-tour-data.tournament-schedule-collect.flow.collect-current-match-schedules 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] HTTP 入口鉴权、当前赛事范围、空范围和成功响应是什么？
  > GET /tour/collect/oop 在 /tour/collect/** 普通鉴权排除范围，无参数。目标为 startDate<=today+1 且 endDate>=today-1，不看 status。空范围循环零次。正常结束统一返回“比赛详情采集完成”，不区分成功、空、跳过。
  → 已写入触发、接口契约、详细流程第 1/7 步与空范围分支

- [Q2] 四种巡回赛/级别如何路由来源，未知 tour、空 category 和双打如何处理？
  > WTA: category GS→ATP_SCHEDULE_FOR_WTA，否则 WTA_SCHEDULE；ATP: GS→ATP_SCHEDULE，否则 ATP_OOP。未知 tour 不进任何分支而静默跳过；ATP/WTA 的 category 为 null 时 equals("GS") NPE。MatchCollectManager 默认 shouldCollect 排除 DOUBLES，tour.collect.doubles=true 才放行来源确实产出的双打。
  → 已写入详细流程第 2-3 步、异常分支与路由线索

- [Q3] 排期时区、“随后”推算、状态及解析失败字段如何处理？
  > 来源各自形成 matchDate/scheduledAt/scheduledAtText/court/courtSeq/round/players/status 等；有时还含 winner/endedAt/sets。带时区值统一转 Asia/Shanghai。followed-by 同场依场序推算：ATP_OOP +100 分钟，ATP/WTA schedule +70 分钟。日期时间轮次种子解析失败为 null；状态按统一映射，未知为 null。
  → 已写入详细流程第 3-4 步及解析失败补充说明

- [Q4] 签表、比赛、球员、参赛如何保存，单项异常与部分提交如何收场？
  > 每个 draw 依次保存 draw、matches、players、entries，身份与签表采集相同，各步骤独立事务/调用且非空覆盖。oop() 没有每赛事 try/catch，任一转换/身份/保存异常终止整个 HTTP 及后续赛事；失败步骤回滚自身，此前赛事和此前步骤保留，无补偿。
  → 已写入详细流程第 5-7 步、业务活动与部分提交分支
