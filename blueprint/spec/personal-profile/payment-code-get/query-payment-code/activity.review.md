# personal-profile.payment-code-get.activity.query-payment-code 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 查询范围和无资料时的响应形态是什么？
  > 按当前 userId 与固定键 wechat_payment_code 唯一查询，不校验账户或档案；无资料时整个业务 data 为 null，不返回空 DTO，也不创建默认资料。
  → 已写入活动契约、详细流程第 1-2 步与边界情况

- [Q2] null、空串、纯空白和普通 key 如何生成地址？
  > 始终原样返回 extValue 为 key；null、空串或纯空白不生成地址，普通值生成 3600 秒七牛签名地址。
  → 已写入业务动作 A2、详细流程第 3 步与边界情况

- [Q3] 是否核验资源格式、存在性或在签名失败时降级？
  > 不核验 key/URL/base64、归属、格式或资源存在性；非空白值签名失败则整次查询 SYSTEM_ERROR，不降级为只返回 key。
  → 已写入异常分支、详细流程第 4-5 步与实现提示
