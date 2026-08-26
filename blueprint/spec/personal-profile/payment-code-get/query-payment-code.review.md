# personal-profile.payment-code-get.flow.query-payment-code 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 没有收款码资料时返回 null、空对象还是业务错误？
  > 返回成功且 Result.data 为 null。domain get 返回 null，MapStruct 生成的 toDTO(null) 也返回 null；不是空 PaymentCodeDTO，也不抛 USER_EXT_NOT_FOUND。
  → 已在契约、详细流程、流程图和服务边界明确无资料成功返回 data=null。

- [Q2] extValue 为 null、空串或纯空白时 key 与 paymentCodeUrl 如何返回？
  > DTO.key 保留 extValue 原值，包括 null、空串或空格；QiniuConfiguration.buildSignedUrl 对 blank 返回 null，因此 paymentCodeUrl 为 null。
  → 已在契约、详细流程和异常说明明确 key 保留原值，空白值只使签名地址为 null。

- [Q3] 查询是否校验账户档案、资源存在、图片类型、资源归属或收款码内容？
  > 全部不校验。只凭 UserContext 的 userId 查询 user_ext；不确认 user/profile 存在，也不访问七牛检查文件，不验证 key 格式、图片类型、资源归属或内容。
  → 已在详细流程和服务边界明确不校验账户档案及任何资源有效性和归属。

- [Q4] 同一用户出现多条固定扩展键记录或七牛签名失败时如何处理？
  > 仓储使用 MyBatis-Plus one 查询，多条匹配会抛系统异常。非空 key 构建签名时若域名、凭据等配置异常或七牛 SDK 构址失败，整次查询系统异常，不降级为只返回 key。
  → 已在流程图、异常分支和技术线索明确多行查询与非空 key 签名失败均终止整次查询。
