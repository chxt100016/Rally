# personal-profile.user-image-upload-authorization.activity.issue-user-image-upload-authorization 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] type 的校验边界与 key 格式是什么？
  > 框架只要求参数存在；空白、未知、超长或路径字符均原样拼入 user/{userId}/{type}_{yyyy-MM-dd_HH-mm-ss}.jpg。
  → 已写入触发条件、业务动作 A1、详细流程第 1-2 步与边界情况

- [Q2] 图片授权的大小、scope 与期限是什么？
  > 固定10MB，scope 精确到生成 key；策略先写600秒 deadline，再以3600秒签发令牌和一小时访问地址。
  → 已写入业务动作 A2-A3 与详细流程第 3 步

- [Q3] 是否读取档案、验证图片或持久化用途与 key？
  > 均不做；不接收文件、不清理旧资源，预览地址也不证明上传成功。
  → 已写入活动契约、详细流程第 4 步与实现提示
