# meetup.meetup-publish.flow.publish-meetup 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 每日发布上限实际统计哪些状态，已结束、已关闭和进行中的约球是否计入？
  > 按 Java 实现确认：从服务器当日零点起只统计创建者本人、状态精确为 OPEN 或小写 full 的记录。ONGOING、FINISHED、CLOSED、DRAFT 和大写 FULL 不计入；达到 anti_abuse.publish_per_day_limit 时拒绝。
  → 已落入详细流程、异常分支和技术线索。

- [Q2] 发布时持续时长与各 NTRP 模式的精确校验和自动补值是什么？
  > 按 Java 实现确认：duration 只接受 0.5、1.0、1.5、2.0、2.5、3.0。RANGE 要求双边、0.5 步长且 min<=max；EXACT 要求 min、0.5 步长并在 max 为空时写成 min；ABOVE 只要求 min，BELOW 只要求 max，未使用的另一边不会清空。Bean Validation 另限制边界 1.5~7.0。
  → 已落入请求参数、详细流程和异常分支。

- [Q3] 命中球场库后，约球编号、城市与创建更新时间最终采用哪些来源？
  > 按 Java 生成映射确认：命中球场库先带入球场 bizId、cityName、createTime、updateTime及场地字段；MeetupFactory 随后重新生成约球 bizId，并按 cityCode 名录覆盖 cityName，但不会重置 createTime/updateTime，所以会沿用球场审计时间。
  → 已落入详细流程和异常分支后的场地说明。

- [Q4] 通知授权场景、重复项和登记失败如何处理，发布接口是否返回新约球编号？
  > 按 Java 实现确认：parseScenes 接受所有 NoticeScene 枚举名，非法项忽略、重复项不去重；grant 内部吞掉异常，不影响事务中的约球、报名和群聊。接口返回 Result<?> 的空成功结果，不返回新约球编号，也不发送通知。
  → 已落入请求参数、成功响应、详细流程和服务边界。
