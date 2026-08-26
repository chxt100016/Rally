# identity-account.account-login.activity.establish-wechat-account 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 既有账户的识别键、返回字段和资料完整性检查是什么？
  > 只按 channel=WECHAT_MINIAPP 与 openid(identifier) 查询；命中就返回其 userId、isNewUser=false，不回查 user 表、不检查资料完整性，也不更新 unionId。
  → 已写入活动契约、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 首次登录建立用户和账户的默认值、顺序与唯一性如何处理？
  > 先建立用户，默认昵称“球员”、默认头像、性别用表默认 UNDISCLOSED；再建立 account，关联 userId，渠道 WECHAT_MINIAPP，identifier=openid，unionId 可空，credential 空。账户唯一性由 channel+identifier 保证。
  → 已写入领域依赖、业务动作 A2-A3 与详细流程第 3-4 步

- [Q3] 并发首次登录或第二步失败是否有事务与补偿？
  > 当前两次仓储写入没有活动级总事务或业务重试；并发唯一冲突或账户创建失败报 SYSTEM_ERROR，已创建用户不保证删除，可能留下孤立用户。
  → 已写入异常分支、详细流程第 5 步与边界情况
