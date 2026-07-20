# 活动费用 · 按人时分摊 接口文档（前端）

约球编辑价格（`/meetup/editPrice`）与活动详情（`/meetup/detail/{meetupId}`）新增“按人时分摊”能力：创建人可以为活动的不同时间段设置不同的参与人员，系统按时长和人数精确计算每个人的应付金额。若未设置该能力，费用仍按原有“人均分摊”逻辑计算。

## 通用约定

- **统一前缀**：所有接口均带 `/api/rally` 前缀。
- **登录态**：均需登录，请求头携带 `Authorization: {token}`。
- **统一响应包装** `Result<T>`：

  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `code` | int | `0` 成功；非 0 为业务错误码 |
  | `message` | string | 错误信息，成功时为 `null` |
  | `data` | T | 业务数据 |

- **相关错误码**：

  | code | message | 说明 |
  | --- | --- | --- |
  | 20001 | 参数错误 | `hourlyAllocations` 校验不通过（见下文校验规则） |

---

## 1. 数据结构

### 1.1 CostItem（费用明细项）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | string | 费用名称，如“场地费” |
| `totalAmount` | int | 该项总金额（分） |

### 1.2 HourlyAllocation（人时分摊段）

一个 `HourlyAllocation` 表示活动中的一段时长，以及这段时长内实际参与的人员。多个 `HourlyAllocation` 的 `duration` 相加必须等于活动总时长（`duration`，单位小时）。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `duration` | number | 该时间段的时长（小时），支持小数，如 `0.5` 表示半小时 |
| `userIds` | string[] | 该时间段内参与的用户 `userId` 列表，必须是活动内的有效参与者 |

**示例**：活动总时长 2 小时，costItems 总额 200 元：

```json
[
  { "duration": 1.0, "userIds": ["u1", "u2"] },
  { "duration": 1.0, "userIds": ["u1", "u2", "u3"] }
]
```

含义：第 1 小时只有 u1、u2 打球（各分摊 50 元）；第 2 小时 u1、u2、u3 一起打（各分摊 33.33 元）。u1 应付总额 = 50 + 33.33 ≈ 83 元。

### 1.3 AllocationModeEnum（分摊模式枚举，详情接口回显）

| 枚举值 | 说明 |
| --- | --- |
| `AVERAGE` | 人均分摊：未设置按人时分摊时的默认模式，按活动人数平均分摊总费用 |
| `HOURLY` | 按人时分摊：设置了 `hourlyAllocations` 时的模式，按每个用户实际参与的时长和人数计算 |

### 1.4 PaymentDTO.UserRoleEnum（当前用户角色枚举，详情接口回显）

| 枚举值 | 说明 |
| --- | --- |
| `COLLECTOR` | 收款人（创建人视角） |
| `PAYER` | 付款人（参与者视角） |
| `STRANGER` | 陌生人（未加入活动，不返回金额相关字段的业务含义） |

---

## 2. 修改活动价格

- **POST** `/api/rally/meetup/editPrice`
- 仅创建人可调用，非创建人调用返回权限错误。

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `meetupId` | string | 是 | 约球 ID |
| `costItems` | CostItem[] | 否 | 费用明细列表 |
| `hourlyAllocations` | HourlyAllocation[] | 否 | 按人时分摊数据；不传或传空数组则使用人均分摊模式 |

### hourlyAllocations 校验规则（不满足则返回 20001 参数错误）

1. 所有 `HourlyAllocation.duration` 之和必须**精确等于**活动的总时长（`duration`）。
2. 每个 `HourlyAllocation.duration` 必须 > 0。
3. 每个 `HourlyAllocation.userIds` 不能为空。
4. `userIds` 中的每个 `userId` 必须是该活动的**当前有效参与者**（已加入，非退出/被拒绝状态），否则报错。

### 请求示例

**不使用按人时分摊（人均模式，兼容旧逻辑）**：

```bash
curl -X POST 'http://localhost:8080/api/rally/meetup/editPrice' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{
    "meetupId": "your_meetup_id",
    "costItems": [
      { "name": "场地费", "totalAmount": 20000 }
    ]
  }'
```

**使用按人时分摊**（活动总时长需为 2.0 小时）：

```bash
curl -X POST 'http://localhost:8080/api/rally/meetup/editPrice' \
  -H 'Content-Type: application/json' -H 'Authorization: {token}' \
  -d '{
    "meetupId": "your_meetup_id",
    "costItems": [
      { "name": "场地费", "totalAmount": 20000 }
    ],
    "hourlyAllocations": [
      { "duration": 1.0, "userIds": ["u1", "u2"] },
      { "duration": 1.0, "userIds": ["u1", "u2", "u3"] }
    ]
  }'
```

响应：`data` 为 `null`。

```json
{ "code": 0, "message": null, "data": null }
```

**清空按人时分摊、退回人均模式**：再次调用 `editPrice`，`hourlyAllocations` 传空数组或不传即可。

---

## 3. 活动详情中的支付回显（PaymentDTO）

- **GET** `/api/rally/meetup/detail/{meetupId}`
- `data.payment` 字段（`PaymentDTO`）承载所有支付相关的回显信息。**仅当活动设置了 `costItems` 时才会返回 `payment`，否则为 `null`。**

### PaymentDTO 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userRole` | UserRoleEnum | 当前登录用户在本次支付中的角色，见 1.4 |
| `costItems` | CostItem[] | 费用明细列表，原样回显 |
| `paymentCodeUrl` | string | 收款人收款码 URL，**仅 `userRole=COLLECTOR` 时**才有意义（当前实现始终尝试返回创建人收款码，前端仅在收款人视角下展示） |
| `totalAmount` | int | 总价（分），由 `costItems` 累加得出 |
| `allocationMode` | AllocationModeEnum | 分摊模式，见 1.3。前端据此判断展示哪种金额说明文案 |
| `currentUserAmount` | int | **当前登录用户**需要支付的金额（分）。`AVERAGE` 模式下 = 总价 / 计算人数；`HOURLY` 模式下按用户实际参与时段精确计算，若当前用户不在任一时段的 `userIds` 中则为 `0` |
| `calculatedPlayerCount` | int | 计算人数，仅 `AVERAGE` 模式下用于展示“按N人计算”；活动未开始用最大人数，已开始/结束用当前实际人数 |
| `hourlyAllocations` | HourlyAllocation[] | 按人时分摊原始数据回显，`allocationMode=HOURLY` 时才非空，用于创建人再次编辑价格时的表单回填 |
| `allocationDesc` | string | 当前登录用户的人时分摊详情文案，仅 `allocationMode=HOURLY` 时才非空，格式如 `"4人2小时、3人1小时"`（按参与人数分组，相同人数的时长累加展示，多组用“、”分隔）；`AVERAGE` 模式下为空字符串 |

### 响应示例

**人均模式**（未设置 `hourlyAllocations`）：

```json
{
  "code": 0,
  "data": {
    "payment": {
      "userRole": "PAYER",
      "costItems": [{ "name": "场地费", "totalAmount": 20000 }],
      "paymentCodeUrl": "https://...",
      "totalAmount": 20000,
      "allocationMode": "AVERAGE",
      "currentUserAmount": 5000,
      "calculatedPlayerCount": 4,
      "hourlyAllocations": null,
      "allocationDesc": null
    }
  }
}
```

**按人时分摊模式**：

```json
{
  "code": 0,
  "data": {
    "payment": {
      "userRole": "PAYER",
      "costItems": [{ "name": "场地费", "totalAmount": 20000 }],
      "paymentCodeUrl": "https://...",
      "totalAmount": 20000,
      "allocationMode": "HOURLY",
      "currentUserAmount": 8333,
      "calculatedPlayerCount": 3,
      "hourlyAllocations": [
        { "duration": 1.0, "userIds": ["u1", "u2"] },
        { "duration": 1.0, "userIds": ["u1", "u2", "u3"] }
      ],
      "allocationDesc": "2人1小时、3人1小时"
    }
  }
}
```

### 前端回显与展示建 议

1. **判断分摊模式**：优先读取 `allocationMode`，而不是靠 `hourlyAllocations` 是否为空来推断（`HOURLY` 模式必然非空，判断更直接）。
2. **金额展示**：
   - 统一使用 `currentUserAmount` 展示“我应付金额”，不要再使用旧字段 `amountPerPerson`（**已删除，前端如仍引用需下线**）。
   - `AVERAGE` 模式下可补充展示 `calculatedPlayerCount`，如“共 {totalAmount/100} 元，按 {calculatedPlayerCount} 人计算，每人 {currentUserAmount/100} 元”。
   - `HOURLY` 模式下展示 `allocationDesc` 作为分摊依据说明，如“我的分摊：{allocationDesc}，共 {currentUserAmount/100} 元”。若 `allocationDesc` 为空字符串，说明当前用户未参与任一时段（`currentUserAmount` 也会是 0），可提示“暂无分摊记录”。
3. **编辑价格表单回显**：创建人再次打开编辑价格页时，用 `hourlyAllocations` 回填时间段表单；若为 `null`/空，则表单展示为“人均分摊”默认态。
4. **收款码展示**：仅 `userRole === 'COLLECTOR'` 时才展示 `paymentCodeUrl` 相关的“分享收款码”入口；`PAYER` 视角下该字段也会返回创建人收款码 URL，用于付款时扫码，需展示；`STRANGER` 视角不展示支付相关内容。
5. **金额单位**：所有金额字段单位均为“分”，前端展示需除以 100 并按需保留两位小数。

---

## 4. 变更摘要（供已有前端代码排查引用）

- **新增**：`MeetupEditPriceCmd.hourlyAllocations`、`PaymentDTO.totalAmount`、`PaymentDTO.allocationMode`、`PaymentDTO.hourlyAllocations`、`PaymentDTO.allocationDesc`。
- **删除**：`PaymentDTO.amountPerPerson`（请全部替换为 `currentUserAmount`）。
- **接口路径与请求方式均未变化**，仅入参/出参字段增减。
