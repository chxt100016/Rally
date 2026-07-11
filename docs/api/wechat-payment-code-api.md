# 微信付款码 API 文档

## 接口概述

用户微信付款码管理接口，支持上传、查询、删除付款码图片。

**Base URL**: `http://localhost:8080/api/rally`

**认证方式**: Bearer Token（请求头 `Authorization: Bearer <token>`）

---

## 接口列表

### 1. 获取上传凭证

获取七牛云上传凭证，用于上传付款码图片。

**接口地址**: `GET /wechat/user/upload/upload-token/user`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值：`paymentCode` |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/rally/wechat/user/upload/upload-token/user?type=paymentCode" \
  -H "Authorization: Bearer <your_token>"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "uploadToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "key": "user/user123/paymentCode_2026-07-11_12-30-45.jpg",
    "maxSizeMb": 10,
    "uploadHost": "https://up-z0.qiniup.com",
    "resourceUrl": "https://cdn.example.com/user/user123/paymentCode_2026-07-11_12-30-45.jpg?e=xxx"
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| uploadToken | String | 七牛云上传凭证 |
| key | String | 文件存储路径（保存到后端时使用）|
| maxSizeMb | Integer | 文件大小限制（MB）|
| uploadHost | String | 七牛云上传地址 |
| resourceUrl | String | 文件访问 URL（预览用）|

**文件命名规则**: `user/{userId}/{type}_{yyyy-MM-dd_HH-mm-ss}.jpg`

---

### 2. 上传到七牛云

使用上一步获取的凭证上传文件到七牛云。

**接口地址**: `POST https://up-z0.qiniup.com`

**请求参数** (multipart/form-data):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 上传凭证（从步骤1获取）|
| key | String | 是 | 文件存储路径（从步骤1获取）|
| file | File | 是 | 图片文件 |

**请求示例**:
```bash
curl -X POST https://up-z0.qiniup.com \
  -F "token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "key=user/user123/paymentCode_2026-07-11_12-30-45.jpg" \
  -F "file=@/path/to/image.jpg"
```

**响应示例**:
```json
{
  "hash": "FmDZwqadA4-ib_15hYfBBY9Z7MLz",
  "key": "user/user123/paymentCode_2026-07-11_12-30-45.jpg"
}
```

---

### 3. 保存付款码

将上传成功的文件 key 保存到后端数据库。

**接口地址**: `POST /wechat/user/payment-code`

**请求参数**:
```json
{
  "key": "user/user123/paymentCode_2026-07-11_12-30-45.jpg"
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/rally/wechat/user/payment-code \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "key": "user/user123/paymentCode_2026-07-11_12-30-45.jpg"
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**业务规则**:
- 如果用户已有付款码，会自动更新（覆盖）
- 一个用户只能有一个付款码

---

### 4. 查询付款码

查询当前登录用户的付款码信息。

**接口地址**: `GET /wechat/user/payment-code`

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/rally/wechat/user/payment-code \
  -H "Authorization: Bearer <your_token>"
```

**响应示例（有付款码）**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "key": "user/user123/paymentCode_2026-07-11_12-30-45.jpg",
    "paymentCodeUrl": "https://cdn.example.com/user/user123/paymentCode_2026-07-11_12-30-45.jpg?e=1720684800&token=xxx"
  }
}
```

**响应示例（无付款码）**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| key | String | 文件存储路径 |
| paymentCodeUrl | String | 带签名的完整访问 URL（有效期1小时）|

---

### 5. 删除付款码

删除当前登录用户的付款码，同时删除七牛云文件。

**接口地址**: `DELETE /wechat/user/payment-code`

**请求示例**:
```bash
curl -X DELETE http://localhost:8080/api/rally/wechat/user/payment-code \
  -H "Authorization: Bearer <your_token>"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**业务规则**:
- 删除数据库记录
- 同时删除七牛云上的文件
- 如果付款码不存在，返回错误码 40013

---

## 完整调用流程

```
1. 获取上传凭证
   GET /wechat/user/upload/upload-token/user?type=paymentCode
   ↓
   响应：{ uploadToken, key, uploadHost }

2. 上传到七牛云
   POST https://up-z0.qiniup.com
   FormData: { token, key, file }
   ↓
   响应：{ key }

3. 保存到后端
   POST /wechat/user/payment-code
   Body: { key }
   ↓
   响应：成功

4. 查询付款码（可选）
   GET /wechat/user/payment-code
   ↓
   响应：{ key, paymentCodeUrl }

5. 删除付款码（可选）
   DELETE /wechat/user/payment-code
   ↓
   响应：成功
```

---

## 错误码

| 错误码 | 错误信息 | 说明 | 解决方案 |
|--------|----------|------|----------|
| 10001 | 未登录，请先登录 | Token 缺失或无效 | 重新登录获取 token |
| 10002 | 登录已过期，请重新登录 | Token 已过期 | 重新登录 |
| 40013 | 用户扩展信息不存在 | 删除不存在的付款码 | 确认用户是否已设置付款码 |
| 40015 | 微信付款码不能为空 | key 字段为空 | 检查上传流程，确保 key 有值 |

---

## 注意事项

### 文件限制
- **大小限制**: 最大 10MB
- **格式**: 支持 JPG/PNG，后端统一生成 `.jpg` 后缀

### URL 有效期
- `paymentCodeUrl` 中的签名有效期为 **1小时**
- 过期后需要重新调用查询接口获取新 URL
- 建议每次展示时都调用查询接口

### 业务规则
- 一个用户只能有一个付款码
- 重复保存会自动覆盖旧的付款码
- 删除操作不可恢复

### 权限控制
- 只能操作当前登录用户自己的付款码
- 通过 token 自动识别用户身份

---

## 测试示例

### 完整上传流程测试

```bash
# 1. 获取上传凭证
TOKEN_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/rally/wechat/user/upload/upload-token/user?type=paymentCode" \
  -H "Authorization: Bearer your_token_here")

# 提取字段
UPLOAD_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.data.uploadToken')
KEY=$(echo $TOKEN_RESPONSE | jq -r '.data.key')
UPLOAD_HOST=$(echo $TOKEN_RESPONSE | jq -r '.data.uploadHost')

echo "Upload Token: $UPLOAD_TOKEN"
echo "Key: $KEY"

# 2. 上传到七牛云
curl -X POST $UPLOAD_HOST \
  -F "token=$UPLOAD_TOKEN" \
  -F "key=$KEY" \
  -F "file=@/path/to/your/image.jpg"

# 3. 保存到后端
curl -X POST http://localhost:8080/api/rally/wechat/user/payment-code \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_token_here" \
  -d "{\"key\": \"$KEY\"}"

# 4. 查询验证
curl -X GET http://localhost:8080/api/rally/wechat/user/payment-code \
  -H "Authorization: Bearer your_token_here"
```

---

## 常见问题

**Q: 上传到七牛云失败怎么办？**  
A: 需要重新获取上传凭证（步骤1），因为凭证有 10 分钟有效期。

**Q: paymentCodeUrl 显示图片失败？**  
A: 签名 URL 有效期 1 小时，过期后需要重新调用查询接口获取新 URL。

**Q: 可以上传多个付款码吗？**  
A: 不可以，一个用户只能有一个付款码。重复保存会自动覆盖。

**Q: 删除后能恢复吗？**  
A: 不能，删除操作会同时删除数据库记录和七牛云文件，无法恢复。

**Q: 支持什么图片格式？**  
A: 支持 JPG/PNG 格式，后端统一生成 `.jpg` 后缀的存储路径。
