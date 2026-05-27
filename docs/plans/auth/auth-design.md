# 用户登录注册系统 · 产品设计规格

**日期：** 2026-05-27  
**平台：** 微信小程序（主）  
**阶段：** MVP

---

## UI 设计稿

| 页面 | 文件 | 说明 |
|------|------|------|
| 登录/注册页 | _(待设计)_ | 手机号注册 + 微信一键登录 |
| 首次进入弹窗 | [../date/ui-onboarding-modal.html](../date/ui-onboarding-modal.html) | 新用户引导，5 步骤：头像/昵称/性别+生日/NTRP 自评/个人简介/打球视频上传（必传） |

---

## 一、注册与登录

用户身份与渠道认证分两张表存储，便于后续绑定多渠道（如同时支持手机号和微信）。

### 数据模型

**users（核心用户）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | string | 系统唯一 ID |
| `nickname` | string | 昵称 |
| `avatar_url` | string | 头像 URL |
| `gender` | enum | `male` / `female` / `undisclosed`，默认 `undisclosed` |
| `birthday` | date | 生日（nullable，用于年龄段筛选，不对外直接展示） |
| `created_at` | datetime | 注册时间 |

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

## 二、首次进入引导（Onboarding）

新用户注册后弹出底部半屏引导弹窗（[ui-onboarding-modal.html](../date/ui-onboarding-modal.html)），分 5 步完善资料：

1. 头像 / 昵称确认（来自微信，可修改）
2. 性别选择（男 / 女 / 不透露）+ 生日填写（选填）
3. NTRP 自评（滚轮选择，1.5~7.0）
4. 个人简介（选填，最多 100 字）
5. 打球视频上传（**必传**，30 秒内；上传后主按钮激活）

用户可点击「先逛逛，稍后完善」跳过，但视频未上传前主按钮保持禁用。

---

## 三、登录页交互

**登录方式：**
- 手机号 + 验证码
- 微信一键登录（`wx.login` 获取 code → 后端换取 openid）

**注册流程：**
1. 手机号输入 → 发送验证码
2. 验证码校验通过 → 创建 `users` + `accounts` 记录
3. 跳转至 Onboarding 引导弹窗

**登录流程：**
1. 手机号 + 验证码 → 查 `accounts` → 返回 token
2. 微信登录 → 后端用 `openid` 查 `accounts`：
   - 已绑定 → 直接返回 token
   - 未绑定 → 创建新用户 → 返回 token + 标记需要 Onboarding

---

## 四、功能边界（MVP 范围）

**包含：**
- 手机号注册（发送验证码）
- 微信小程序一键登录（`wx.login`）
- accounts 表多渠道绑定
- Onboarding 引导：头像/昵称/性别/生日/NTRP 自评/个人简介/打球视频（必传）
- JWT token 签发与校验

**不包含（后续迭代）：**
- 微信与手机号账号合并（unionid 去重）
- 第三方社交平台登录
- 密码登录
- 短信通道选择
