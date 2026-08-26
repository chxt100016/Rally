# identity-account.registration-profile-completion.activity.complete-registration-profile 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 昵称、头像与当前用户如何校验，用户不存在时报什么？
  > 登录由流程校验；nickname 与 avatarUrl 必须非空白，否则 PARAM_ERROR。活动以登录上下文 userId 更新，仓储未找到用户时当前以 SYSTEM_ERROR 处理，不建立新用户。
  → 已写入活动契约、异常分支、业务动作 A2 与详细流程第 2 步

- [Q2] 生日、性别、城市未提交与空字符串分别如何更新？
  > birthday 从 yyyy-MM-dd HH:mm:ss 解析为 LocalDateTime，映射到用户时只保留日期；gender、birthday、cityCode 为 null 时更新映射忽略并保留原值；cityCode 空字符串不是 null，会覆盖原值。
  → 已写入活动契约、业务动作 A1-A2、详细流程第 1、3 步与边界情况

- [Q3] 重复提交、默认占位值与并发覆盖如何决定资料完善？
  > 不校验新用户身份，不检查默认昵称/头像，也无独立完成标记；相同内容可重复更新。其他业务仍按是否为默认占位值判断完善状态；无版本控制，并发时后完成写覆盖先写。
  → 已写入触发条件、详细流程第 4-5 步与边界情况
