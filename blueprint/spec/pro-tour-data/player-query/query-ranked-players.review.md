# pro-tour-data.player-query.flow.query-ranked-players 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 接口鉴权、tour 缺失/空白/大小写与非法值如何处理？
  > GET /tour/player/players 在鉴权排除清单中。tour query param 缺失走全局 OPERATION_FAILED；isBlank 为真直接空列表。非空仅 toUpperCase，不 trim、不限制 ATP/WTA，未知值通常空列表。
  → 接口契约、详细流程第 1-2 步与参数异常分支

- [Q2] 哪些球员进入结果，排序、分页和返回字段是什么？
  > SQL 精确匹配 tour 且 rank IS NOT NULL，rank ASC，无同排名次序、分页或上限。返回 id/rank/name/country/points/age/birthDate，不含 tour、头像、性别、持拍手。
  → 接口契约、详细流程第 3-4 步与服务边界

- [Q3] 姓名、国家地区、出生日期和年龄的精确组装与异常数据规则是什么？
  > name=(firstName或空)+空格+(lastName或空) 后 trim，两者空则空串。country null 则空；未知码原码作 code/name、flagCode null。birthDate 存在用查询当日 Period 年数并格式 yyyy-MM-dd；未来日期可得负年龄，缺失则 age/birthDate null。
  → 详细流程第 5-7 步与异常补充说明

- [Q4] 简中译文为空、缺失或保存冲突时如何处理，是否有持久化副作用？
  > 按完整 name 查 PLAYER/zh-CN。非空译文替换；已有空 translated_text 时缓存返回 null 的具体行为等同未提供译文，但数据库存在记录使重复保存可能冲突并被逐条吞掉。完全 miss 也逐条尝试新增；原文始终继续交付。
  → 详细流程第 8-9 步、业务活动与翻译异常说明

- [Q5] 球员或翻译读取失败时是否返回部分列表，已登记待译项是否回滚？
  > 球员查询、映射或翻译缓存未处理异常终止整体，不返回部分列表。待译保存本身逐条 catch；在后续步骤异常前已经成功保存的条目没有共享事务，不回滚。
  → 详细流程第 9 步与 OPERATION_FAILED 分支
