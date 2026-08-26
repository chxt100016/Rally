# pro-tour-data.tournament-draw-collect.flow.collect-current-tournament-draws 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 当前赛事入口的鉴权、日期窗口、状态条件和成功响应是什么？
  > GET /tour/collect/currentDraws 在普通鉴权排除范围，无参数。按本地 today 查询 startDate<=today+1 且 endDate>=today-1，不看 status。空列表正常结束。无论单项成功失败，遍历结束均返回纯文本“当前签表采集完成”，不含统计。
  → 已写入触发、接口契约、详细流程第 1/7 步及空范围分支

- [Q2] 各赛事如何选择来源，WTA 为什么还有已完成赛果补充？
  > 每项 ATP GS→ATP_APP_DRAW，ATP 非GS→ATP_DRAW；WTA GS→ATP_APP_DRAW，WTA 非GS→WTA_DRAW；所有 WTA 再调用 ATP_APP_COMPLETED，用该来源补充已结束比赛快照。来源空不更新，tour/category 非法在该项抛错。
  → 已写入详细流程第 2-3/6 步及路由技术线索

- [Q3] 单项赛事失败是否阻止其他赛事，调用方能否看到失败明细？
  > currentDraws 对每个 tournament 包裹 try/catch，单项异常记日志后继续其余赛事。当前赛事列表读取若失败则请求整体失败；逐项失败被吞掉，因此最终成功字符串无法揭示部分或全部失败，没有失败清单或重试。
  → 已写入详细流程第 6-7 步、流程图和单项异常分支

- [Q4] 签表内各对象的保存顺序、身份、覆盖与部分提交规则是什么？
  > 每份 draw 依次 saveOrUpdate 签表、saveMatches、savePlayers、saveEntries。身份分别为 tournamentId+year+drawType、drawId+matchId、tour+playerId、drawId+playerId。存量主要非空覆盖，来源遗漏不删除，状态快照可回退。步骤使用独立事务/调用，后续失败保留此前提交。
  → 已写入详细流程第 4-5 步、业务活动及部分提交说明
