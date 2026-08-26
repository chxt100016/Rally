# platform-config.splash-cover-query.activity.issue-splash-cover-url 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 无覆盖、停用覆盖和启用空白覆盖分别得到什么？
  > 无覆盖或停用回退 default/splash-cover-20260821.jpg；启用覆盖值若null/空串/纯空白则直接返回null，不回退。
  → 已写入业务动作 A1-A2、详细流程第 2-3 步与边界情况

- [Q2] 签名使用哪些配置和期限？
  > domain决定协议并构址，access/secret签名到当前Unix秒+3600，bucket不参与。
  → 已写入业务动作 A2 与详细流程第 4 步

- [Q3] 可选登录和对象存在性是否影响结果？
  > 身份不影响选择，可选鉴权失败按匿名继续；不检查对象存在性，签名成功也不保证URL可打开。
  → 已写入触发条件、详细流程第 1、5 步与边界情况
