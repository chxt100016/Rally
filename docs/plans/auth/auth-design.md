# 用户登录注册系统 · 产品设计规格

**日期：** 2026-05-27  
**平台：** 微信小程序（主）  
**阶段：** MVP

---

## UI 设计稿

| 页面 | 文件 | 说明 |
|------|------|------|
| 登录/注册页 | _(待设计)_ | 手机号注册 + 微信一键登录 |

---

## 一、数据模型

用户身份与渠道认证分两张表存储，便于后续绑定多渠道（如同时支持手机号和微信）。

**users（核心用户）**

| 字段           | 类型       | 说明                                                 |
|--------------|----------|----------------------------------------------------|
| `user_id`    | string   | 系统唯一 ID                                            |
| `nickname`   | string   | 昵称                                                 |
| `avatar_url` | string   | 头像 URL                                             |
| `gender`     | enum     | `male` / `female` / `undisclosed`，默认 `undisclosed` |
| `birthday`   | date     | 生日（nullable，用于年龄段筛选，不对外直接展示）                       |
| `phone`      | string   | 手机号                                                |
| `邮箱`         | string   | 邮箱                                                 |
| `created_at` | datetime | 注册时间                                               |

**accounts（渠道认证，一对多）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `account_id` | string | 唯一 ID |
| `user_id` | string | 关联 users.user_id |
| `channel` | enum | `phone` / `wechat_miniapp`（可扩展） |
| `identifier` | string | 渠道唯一标识：手机号 或 wechat_openid |
| `credential` | string | 凭证：密码哈希 或 access_token（nullable） |
| `wechat_unionid` | string | 微信跨端识别，仅微信渠道填写（nullable） |
| `created_at` | datetime | 绑定时间 |

> `(channel, identifier)` 建唯一索引，防止同一渠道账号重复绑定。

---

## 二、登录页交互

**登录方式：**
- 手机号 + 验证码
- 微信一键登录（`wx.login` 获取 code → 后端换取 openid）

**注册流程：**
1. 手机号输入 → 发送验证码
2. 验证码校验通过 → 创建 `users` + `accounts` 记录
3. 跳转至 Onboarding 引导弹窗（见 [date-design.md](../date/date-design.md)）

**登录流程：**
1. 手机号 + 验证码 → 查 `accounts` → 返回 token
2. 微信登录 → 后端用 `openid` 查 `accounts`：
   - 已绑定 → 直接返回 token
   - 未绑定 → 创建新用户 → 返回 token + 标记需要 Onboarding

---

## 三、功能边界（MVP 范围）

**包含：**
- 微信小程序一键登录（`wx.login`）
- accounts 表多渠道绑定
- JWT token 签发与校验

**不包含（后续迭代）：**
- 手机号注册（发送验证码）
- 微信与手机号账号合并（unionid 去重）
- 第三方社交平台登录
- 密码登录
- 短信通道选择
- phone、email 信息收集
