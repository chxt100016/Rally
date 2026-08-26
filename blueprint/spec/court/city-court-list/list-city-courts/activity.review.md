# court.city-court-list.activity.list-city-courts 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 城市编码是否核对城市名录，空白和不存在分别如何处理？
  > 空白由流程参数校验报 PARAM_ERROR；活动不查询城市名录，非空但不存在的编码按成功返回空列表。
  → 已写入触发条件、活动契约、详细流程第 1 步与边界情况

- [Q2] 球场筛选、排序、分页与并发视图如何定义？
  > 仅筛选 city_code 精确相等且 status=ACTIVE 的记录；不分页、不限制、不追加排序。一次仓储查询形成响应视图，并发修改只按数据库查询时可见结果体现。
  → 已写入业务动作 A1、详细流程第 2 步与边界情况

- [Q3] 别名、标签、环境展示和 ext_data 解析失败如何降级？
  > 返回当前 DTO 全字段；alias/tags 按存储分隔规则转列表，type 映射中文 typeShow，ext_data 补 pinyin/pinyinInitial/rating/cost/opentime/tel。字段缺失或 JSON 无法识别时相应扩展字段为空，不丢弃球场。
  → 已写入活动契约、业务动作 A2-A3、详细流程第 3-5 步
