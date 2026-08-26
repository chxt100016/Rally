# @identity.user-extension 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 覆盖同一 userId 与 extKey 的扩展资料时，bizId 是否应随每次保存更换？
  > 不应更换。bizId 是扩展记录聚合标识，首次建立后保持不变；覆盖只替换 extValue 并更新时间。
  → 聚合根、I2 与 C1

- [Q2] 删除收款码在外部文件删除与数据库记录删除之间发生并发覆盖时，如何避免误删新记录？
  > 读取时返回 bizId 与 extValue 快照；数据库删除必须带 expectedBizId 和 expectedValue 条件。并发覆盖后条件不匹配则拒绝删除，不能按 userId 与 extKey 盲删新值。
  → I4、C2 与并发覆盖边界情况

- [Q3] wechat_payment_code 的 extValue 应强制为对象存储 key，还是兼容历史 URL/base64？
  > 领域层将其视为非空白的外部资源引用字符串，兼容历史 key、URL 或 base64，不臆测格式和归属；外部删除仅对基础设施可识别的对象 key 执行。
  → 外部引用、I3 与历史值边界情况
