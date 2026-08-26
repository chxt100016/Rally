# meetup.meetup-square-list.activity.search-available-meetups-by-time 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 开放、未结束、类型、时间与水平筛选的精确口径是什么？
  > 固定 city_code、存储 status='OPEN'、end_time>当前时刻；matchType 精确，start/end 为开球时间闭区间。仅查询 levelMin 和 levelMax 同时存在时按区间交集，约球 null 边界视无限；levelMode/tags 忽略。
  → 已写入活动契约、业务动作 A1、详细流程第 1-3 步与边界情况

- [Q2] 半径和位置参数如何校验与计算，异常值如何处理？
  > 仅 radiusKm 非空时要求 lng/lat，将公里乘 1000 后用 ST_Distance_Sphere 过滤。当前不校验坐标范围、半径正数和上限；非法坐标可能数据库失败，零/负半径可能无结果。
  → 已写入触发条件、异常分支、详细流程第 4 步与边界情况

- [Q3] 复合游标、排序和多取一项如何保证翻页？
  > 时间游标解成 lastBizId+lastStartTime，查询 start_time 更晚或同时间 biz_id 更大的记录，按相同两键升序，LIMIT pageSize+1。无 lastStartTime 时不应用游标；多一项由上层判 hasMore。
  → 已写入活动契约、业务动作 A2-A3、详细流程第 5-6 步与实现提示
