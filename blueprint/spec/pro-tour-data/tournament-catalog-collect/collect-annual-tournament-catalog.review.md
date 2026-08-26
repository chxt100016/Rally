# pro-tour-data.tournament-catalog-collect.flow.collect-annual-tournament-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 入口鉴权、year 参数限制与成功响应是什么？
  > GET /tour/collect/tournaments 在 /tour/collect/** 鉴权排除范围，匿名可调用。year 必填且须转换为 Integer，无上下限或合理性校验。Controller 返回 void，完整执行或可接受的空来源分支以 HTTP 成功空响应体结束，不返回数量。
  → 已写入触发、接口契约、详细流程第 1/7 步及参数异常分支

- [Q2] ATP 来源 null、空列表、转换或保存失败分别如何影响 WTA？
  > AtpTvClient 捕获请求/解析异常并返回 null；atp() 虽记录空警告但随后对 infos.stream，null 会 NPE 并阻止 WTA。真正的空 List 可转换为空并由保存层直接返回，随后继续 WTA。ATP 转换或事务保存异常同样终止，ATP 当批回滚，WTA 不执行。
  → 已写入详细流程第 2-4 步、流程图及 ATP 异常分支

- [Q3] WTA 来源失败或空内容如何收场，ATP 已保存资料是否回滚？
  > WtaClient 捕获请求/解析异常并返回 null；wta() 对 response null 或 content 空直接 warn+return，因此 HTTP 仍成功结束。WTA 转换或保存的未处理异常才整体失败。ATP 是前一次独立事务，成功提交后不因 WTA 失败或跳过而回滚。
  → 已写入详细流程第 5-7 步、流程图及 WTA 异常分支

- [Q4] 赛事身份、字段覆盖、图片保留及来源遗漏赛事规则是什么？
  > 存量匹配键是 tournamentId+year，不包含 tour；同键可跨来源覆盖。新记录保留来源全部映射，更新覆盖 name/tour/category/surface/city/country/prizeMoney/prizeMoneyText/status/startDate/endDate，但不改 imagePath/backgroundPath。本次未出现的存量记录不处理；批内未显式去重。
  → 已写入详细流程第 4/7 步、服务边界与身份键技术线索

- [Q5] ATP/WTA 的范围、状态、级别、日期和奖金映射规则是什么？
  > ATP 查询全年范围 size=200，不分页，统一 tour ATP/status active；级别照收 type，奖金文本取 prize，数值删除非数字后转 Integer，日期 ISO 解析失败为 null。WTA page0/pageSize1000/exclude ITF；past→completed，其他→active；含 Grand Slam→GS，否则 level 去 WTA 前缀；long 奖金直接转 int，文本为数值+币种，日期非法为 null。
  → 已写入详细流程第 2-6 步、异常补充说明及来源技术线索
