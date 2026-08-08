# 微信手机号授权接口

## 接口说明

微信小程序通过 `button open-type="getPhoneNumber"` 获取一次性动态 `code`，将该 code 提交给后端。后端调用微信 `phonenumber.getPhoneNumber` 接口换取手机号，并保存到当前登录用户的 `user.phone`。

- 动态 code 有效期为 5 分钟，且只能消费一次。
- 该 code 与 `wx.login` 返回的 code 不同，不能混用。
- 本接口只保存用户手机号，不创建手机号登录账号。

## 提交手机号授权 code

- Method: `POST`
- URL: `/api/rally/wechat/user/phone`
- Header: `Authorization: Bearer <token>`
- Content-Type: `application/json`

请求体：

```json
{
  "code": "getPhoneNumber 返回的动态 code"
}
```

成功响应：

```json
{
  "code": 0,
  "message": null,
  "data": null
}
```

curl：

```bash
curl -X POST 'http://localhost:9482/api/rally/wechat/user/phone' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"code":"<getPhoneNumber动态code>"}'
```

## 常见失败

| 业务码 | 说明 |
| --- | --- |
| `10001` | 未登录 |
| `20001` | code 为空 |
| `30002` | 获取微信 access_token 失败 |
| `30003` | 动态 code 无效、过期、已消费，或微信未返回手机号 |
| `40012` | 当前用户不存在 |
