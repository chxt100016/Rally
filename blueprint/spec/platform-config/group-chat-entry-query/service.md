---
id: platform-config.group-chat-entry-query
---

## 服务边界

本服务只向已登录用户交付固定对象 `default/qrcode.jpg` 的一小时七牛签名地址。它不读取或维护群聊二维码配置，不上传、替换、删除或校验存储对象，也不判定用户的入群资格或扫码结果。
