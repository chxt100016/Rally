# meetup.my-meetups.activity.pack-user-meetup-cards 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 五个标签的主标签文案分别如何选择？
  > PENDING 用 pendingReason 文案；IN_PROGRESS/COMPLETED 用 districtName；MY_PUBLISH/RECENT 对已过 endTime 的 OPEN 显示 FINISHED 文案，否则用存储状态文案。状态字段本身不改。
  → 已写入业务动作 A1-A2、详细流程第 1-3 步与边界情况

- [Q2] 球场背景如何读取、选择和降级？
  > 有 courtId 时读 rally_court 的 type/surface，结合开始时段和固定晴天选择背景；球场或字段缺失降级为室外硬地晴天。
  → 已写入业务动作 A3、详细流程第 4 步、边界情况与实现提示

- [Q3] total、hasMore、nextCursor 与只读副作用如何处理？
  > total 固定 null，hasMore 沿用仓储；仅 hasMore=true 且列表非空时把末项 meetupId 编码成游标。活动不清未读、不更新评价或状态。
  → 已写入活动契约、业务动作 A4、详细流程第 5-6 步与边界情况
