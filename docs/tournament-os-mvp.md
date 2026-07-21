# Tournament OS（赛事操作系统）MVP 方案 V2

> 核心理念：**线上完成资格赛，线下完成正赛决赛。**
> 平台不组织比赛，只负责组织流程：状态流转、协商、匹配、记录。
> 产品目标：用"渐进式晋级"取代传统线下赛"一天打完、长期等待"的体验，营造持续的紧张感，同时用于前期推广约球平台。

---

## 一、赛事生命周期

```text
创建赛事（草稿）
      │
      ▼
激活赛事 → 报名池开放（到报名开始时间）
      │
      ▼
资格赛（到资格赛开始时间，凌晨2点定时批量匹配）
      │
      ▼
产生正赛资格 → 支付报名费锁定席位
      │
      ▼
正赛（线上，逐轮随机匹配）
      │
      ▼
指定轮次转线下（如四强）
      │
      ▼
冠军
```

---

## 二、数据表设计

### 1. rally_tournament（赛事表）

```sql
DROP TABLE IF EXISTS `rally_tournament`;
CREATE TABLE `rally_tournament` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `tournament_name` VARCHAR(128) NOT NULL COMMENT '赛事名称',
    `poster_key` VARCHAR(256) DEFAULT NULL COMMENT '活动海报图片key（对象存储）',
    `city_code` VARCHAR(16) NOT NULL COMMENT '城市编码',
    `city_name` VARCHAR(32) NOT NULL COMMENT '城市名称',
    `ntrp_level` VARCHAR(16) NOT NULL COMMENT 'NTRP等级：3.0/3.5/4.0...',
    `gender_limit` VARCHAR(16) NOT NULL COMMENT '性别限制：ALL/MALE/FEMALE',
    `total_slots` INT NOT NULL COMMENT '正赛签位：16/32/64',
    `offline_from_round` INT NOT NULL COMMENT '几强后转线下：4/8/16',
    `qualifier_group_size` INT NOT NULL DEFAULT 2 COMMENT '资格赛每组人数，默认2，可设3',
    `entry_fee` BIGINT NOT NULL COMMENT '报名费，单位：分',
    `registration_start_time` DATETIME NOT NULL COMMENT '报名开始时间',
    `registration_end_time` DATETIME DEFAULT NULL COMMENT '报名截止时间，可空',
    `qualifier_start_time` DATETIME NOT NULL COMMENT '资格赛开始时间',
    `qualifier_end_time` DATETIME DEFAULT NULL COMMENT '资格赛截止时间，可空表示永久有效',
    `qualifier_reject_limit` INT NOT NULL DEFAULT 1 COMMENT '资格赛阶段拒绝次数上限',
    `main_draw_reject_limit` INT NOT NULL DEFAULT 1 COMMENT '正赛阶段拒绝次数上限',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/ACTIVE/ABANDONED',
    `current_filled_slots` INT NOT NULL DEFAULT 0 COMMENT '当前已支付锁定的正赛席位数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事表';
```

### 2. rally_tournament_entry（报名表）

```sql
DROP TABLE IF EXISTS `rally_tournament_entry`;
CREATE TABLE `rally_tournament_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `tournament_id` VARCHAR(32) NOT NULL COMMENT '赛事bizId',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `partner_id` VARCHAR(32) DEFAULT NULL COMMENT '双打搭档用户ID',
    `preferred_districts` VARCHAR(512) DEFAULT NULL COMMENT '活动区域，JSON数组',
    `court_ability` VARCHAR(16) NOT NULL COMMENT '场地能力：CAN_BOOK/CANNOT_BOOK',
    `available_times` VARCHAR(512) DEFAULT NULL COMMENT '可比赛时间，JSON数组',
    `status` VARCHAR(24) NOT NULL DEFAULT 'QUALIFIER_WAITING_MATCH' COMMENT '状态：QUALIFIER_WAITING_MATCH/QUALIFIER_IN_MATCH/AWAIT_PAYMENT/MAIN_DRAW_WAITING_MATCH/MAIN_DRAW_IN_MATCH/ELIMINATED',
    `current_round` VARCHAR(16) NOT NULL DEFAULT 'QUALIFIER' COMMENT '当前轮次：QUALIFIER/ROUND_32/ROUND_16/ROUND_8/ROUND_4/FINAL',
    `qualifier_reject_count` INT NOT NULL DEFAULT 0 COMMENT '资格赛阶段已拒绝次数',
    `main_draw_reject_count` INT NOT NULL DEFAULT 0 COMMENT '正赛阶段已拒绝次数',
    `qualified_time` DATETIME DEFAULT NULL COMMENT '获得正赛资格时间',
    `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间（正赛席位锁定时间）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    UNIQUE KEY `uk_tournament_user` (`tournament_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事报名表';
```

昵称、NTRP等级、性别等用户基础信息不冗余存储，需要时通过 `user_id` 查用户域。状态拆分为资格赛/正赛两条线：`QUALIFIER_WAITING_MATCH`（资格赛排队匹配）、`QUALIFIER_IN_MATCH`（资格赛比赛中）、`AWAIT_PAYMENT`（资格赛获胜，待支付锁定正赛席位）、`MAIN_DRAW_WAITING_MATCH`（支付成功，正赛排队匹配）、`MAIN_DRAW_IN_MATCH`（正赛比赛中）、`ELIMINATED`（淘汰）。拒绝次数按阶段分别计数，对应赛事配置的 `qualifierRejectLimit` 和 `mainDrawRejectLimit` 分别判断上限。

### 3. rally_tournament_match（比赛表）

```sql
DROP TABLE IF EXISTS `rally_tournament_match`;
CREATE TABLE `rally_tournament_match` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `tournament_id` VARCHAR(32) NOT NULL COMMENT '赛事bizId',
    `round` VARCHAR(16) NOT NULL COMMENT '轮次：QUALIFIER/ROUND_32/...',
    `group_size` INT NOT NULL DEFAULT 2 COMMENT '本场人数（2或3）',
    `court_booker_id` VARCHAR(32) DEFAULT NULL COMMENT '订场人用户ID',
    `court_booker_selected_time` DATETIME DEFAULT NULL COMMENT '订场人确定时间',
    `court_name` VARCHAR(128) DEFAULT NULL COMMENT '球场名称',
    `court_address` VARCHAR(256) DEFAULT NULL COMMENT '球场地址',
    `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '约定开始时间',
    `scheduled_end_time` DATETIME DEFAULT NULL COMMENT '约定结束时间',
    `meetup_id` VARCHAR(32) DEFAULT NULL COMMENT '关联约球活动bizId',
    `winner_id` VARCHAR(32) DEFAULT NULL COMMENT '晋级者用户ID',
    `submitted_by` VARCHAR(32) DEFAULT NULL COMMENT '结果提交人用户ID',
    `submitted_time` DATETIME DEFAULT NULL COMMENT '结果提交时间',
    `reject_phase` VARCHAR(16) DEFAULT NULL COMMENT '终止比赛的拒绝发生阶段：SCHEDULE_REJECT(拒绝比赛)/RESULT_REJECT(拒绝结果)，仅终止比赛时写入',
    `reject_reason_code` VARCHAR(32) DEFAULT NULL COMMENT '拒绝理由编码，见理由预设表',
    `reject_reason_text` VARCHAR(256) DEFAULT NULL COMMENT '理由为"其他"时的自由文本',
    `rejected_by` VARCHAR(32) DEFAULT NULL COMMENT '拒绝人用户ID',
    `rejected_time` DATETIME DEFAULT NULL COMMENT '拒绝时间',
    `last_rebook_by` VARCHAR(32) DEFAULT NULL COMMENT '最近一次打回重订的用户ID，打回不终止比赛，不留历史只记最近一次',
    `last_rebook_reason_code` VARCHAR(32) DEFAULT NULL COMMENT '打回理由编码',
    `last_rebook_reason_text` VARCHAR(256) DEFAULT NULL COMMENT '打回理由为"其他"时的自由文本',
    `last_rebook_time` DATETIME DEFAULT NULL COMMENT '打回时间',
    `status` VARCHAR(16) NOT NULL DEFAULT 'MATCHED' COMMENT '状态：MATCHED/BOOKING/SCHEDULED/PENDING_CONFIRM/COMPLETED/REJECTED',
    `matched_time` DATETIME NOT NULL COMMENT '匹配时间',
    `completed_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，防止订场人身份并发抢占等重复操作',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    KEY `idx_tournament_round` (`tournament_id`, `round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事比赛表';
```

去掉了 `PLAYING` 状态：约球是否进行中直接看关联 `meetupId` 的约球状态，Match 停留在 `SCHEDULED` 直到有人回落地页提交结果。

`version` 字段用于乐观锁：选择订场人身份时（双方都可订场，先到先得场景）等并发写操作，均按 `version` 做条件更新，更新影响行数为0则说明已被对方抢先操作，返回业务错误提示前端刷新状态。`rally_tournament_match_participant` 表因为每条记录只由该记录对应的用户本人写入，不存在同行并发竞争，不需要乐观锁。

#### 打回重订 vs 拒绝比赛（SCHEDULED 阶段两种不同操作）

`SCHEDULED` 阶段，非订场人看到赛约信息后，有两个语义不同的操作：

- **打回重订**：不终止比赛，只是让订场人重新提交场地和时间。状态退回 `BOOKING`，所有参与者的 `confirm_status` 重置为 `PENDING`。不设次数上限，不计入拒绝次数，只记录最近一次打回信息（`last_rebook_*` 字段），用于给订场人展示"上次为什么被打回"。
- **拒绝比赛**：终止本场比赛，全员回到匹配池，等待下次批量匹配。写入 `reject_phase = SCHEDULE_REJECT`，计入该用户在当前阶段（资格赛/正赛）的拒绝次数。

`PENDING_CONFIRM` 阶段（结果确认）只有"拒绝结果"一种终止操作，写入 `reject_phase = RESULT_REJECT`。

#### 拒绝理由预设

两种终止性拒绝都必须选择理由，不能空着拒绝：

```text
拒绝比赛（SCHEDULE_REJECT）：
- 时间/场地实在协调不了
- 不想打了
- 其他（需填自由文本）

拒绝结果（RESULT_REJECT）：
- 不服，我要申诉重来
- 提交的结果不属实
- 其他（需填自由文本）
```

打回重订（不终止）的理由预设：

```text
- 时间不合适
- 地点不合适
- 其他（需填自由文本）
```

"不服，我要申诉重来"是刻意保留的产品设计：业余比赛氛围下，允许输家有一次不服气重来的机会，本质是把"拒绝结果"包装成一种有限额度的申诉权（受 `mainDrawRejectLimit`/`qualifierRejectLimit` 硬限制），用完即止，不是无成本翻盘。"提交的结果不属实"则是另一种性质——纠正记录错误，两者理由分开记录，方便后续观察数据分布。

### 4. rally_tournament_match_participant（比赛参与者表，新增）

因为一场比赛可能是2人、3人（`qualifierGroupSize` 决定）或双打4人，需要单独一张表记录每个参与者的确认状态。

```sql
DROP TABLE IF EXISTS `rally_tournament_match_participant`;
CREATE TABLE `rally_tournament_match_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `match_id` VARCHAR(32) NOT NULL COMMENT '关联比赛bizId',
    `tournament_id` VARCHAR(32) NOT NULL COMMENT '赛事bizId（冗余，便于查询）',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `team_id` VARCHAR(32) DEFAULT NULL COMMENT '同队标识，双打时同队两条记录的team_id相同；单打为空',
    `confirm_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '赛约确认状态：PENDING/CONFIRMED/REJECTED',
    `confirm_time` DATETIME DEFAULT NULL COMMENT '赛约确认时间',
    `result_confirm_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '结果确认状态：PENDING/CONFIRMED/REJECTED',
    `result_confirm_time` DATETIME DEFAULT NULL COMMENT '结果确认时间',
    `is_winner` TINYINT(1) DEFAULT NULL COMMENT '是否晋级，流转到COMPLETED时按winnerId统一置位',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    UNIQUE KEY `uk_match_user` (`match_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事比赛参与者表';
```

昵称、性别等不冗余，需要时通过 `user_id` 查用户域。双打场景下同一队的两名选手各自一条记录（各自独立确认/拒绝），`team_id` 相同标识同队；单打每人一条，`team_id` 为空。提交比赛结果时"谁赢了"的选项按 `team_id` 分组展示为一个选项（而不是罗列4个用户），晋级判定时把同 `team_id` 的所有记录一并置 `is_winner`。

- 单打1v1：2条记录；单打3人组：3条记录；双打1v1：4条记录（两队各2人，`team_id` 两两相同）
- 赛约阶段：所有人 `confirmStatus = CONFIRMED` → 流转 `SCHEDULED`
- 赛约阶段任意一人"打回重订" → 不终止，`confirmStatus` 全部重置为 `PENDING`，`match.status` 退回 `BOOKING`
- 赛约阶段任意一人"拒绝比赛" → 整组 `REJECTED`，所有人回到匹配池，拒绝次数记在拒绝的那个人身上
- 结果阶段：提交人自动 `resultConfirmStatus = CONFIRMED`，其余人逐个确认，全部 `CONFIRMED` → 流转 `COMPLETED`
- 结果阶段任意一人"拒绝结果" → 整组 `REJECTED`，所有人回到匹配池，拒绝次数记在拒绝的那个人身上

### 5. rally_meetup（约球表，新增字段）

```sql
ALTER TABLE `rally_meetup`
    ADD COLUMN `match_id` VARCHAR(32) DEFAULT NULL COMMENT '关联的比赛bizId，普通约球为空' AFTER `biz_id`,
    ADD COLUMN `meetup_type` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '约球类型：NORMAL/TOURNAMENT' AFTER `match_id`;
```

约球其他功能保持不变，不与比赛耦合——约球详情页只负责场地、时间、约球本身的流程；输赢判定和比分记录都在比赛落地页完成，因为一次约球可能包含热身赛，约球本身的比分不能代表比赛胜负。

### 6. ScoreRecordPO（比分表，已有）

关联 `rallyMeetupId`，如有需要记录详细比分可继续复用，双打字段已具备（`sideAPlayer1/2`、`sideBPlayer1/2`），比赛结果判定不依赖此表，只用于事后查看详细比分。

---

## 三、匹配机制

### 定时任务
```text
每天凌晨 2:00 执行批量匹配（Cron: 0 0 2 * * ?）
只有到达 qualifierStartTime 之后才开始跑资格赛匹配
```

### 匹配逻辑作为领域能力（不写在SQL里）

```text
Repository 职责：只查询候选人列表（WAITING_MATCH + 当前轮次）
Domain Service（TournamentMatchingService）职责：
  输入：候选人列表 + qualifierGroupSize + 拒绝历史
  输出：分组结果 List<MatchGroup>

算法步骤：
1. 活动区域交集判断（代码里 Set 交集运算）
2. 排除互相拒绝过的组合（除非无法凑齐，兜底强制匹配，如最后剩2人）
3. 按 groupSize 随机分组（Collections.shuffle + 分批）
4. 凑不齐一组的人暂时放回候选池，等下次匹配
```

---

## 四、比赛状态机

```text
[MATCHED]
    ↓ 订场人确定（先到先得，完美情况默认指定，可放弃）
[BOOKING]
    ↓ 订场人提交场地和时间
[SCHEDULED]
    ├─ 全部人接受（confirmStatus 全部 CONFIRMED）→ 创建 Meetup → 待比赛
    ├─ 任意人"打回重订" → 退回 [BOOKING]（不终止）
    └─ 任意人"拒绝比赛" → [REJECTED]（终止，全员回匹配池）
待比赛（约球线下进行，用户回落地页操作）
    ↓
[PENDING_CONFIRM]（任意一人提交"谁赢了"）
    ├─ 其余人逐个确认（resultConfirmStatus 全部 CONFIRMED）→ [COMPLETED]
    └─ 任意人"拒绝结果" → [REJECTED]（终止，全员回匹配池）
```

所有以下操作均在比赛落地页的当前待办区完成，由 `actionState` 决定当前展示哪一步。

### 1. MATCHED：订场人选择
- 所有参与者都能看到"我来订场"按钮
- 完美情况（一人 CAN_BOOK，其余 CANNOT_BOOK）默认指定该人为订场人，但可以主动"放弃订场权"
- 都能订场或都不能订场时，先点击的人获得订场身份（乐观锁防止双方同时抢占）

### 2. BOOKING：订场
- 订场人视角：用已有的球场选择组件，选场地+时间，**不需要**在此确定比赛形式（线下自行协商）
- 非订场人视角：只看到"对方正在订场"的等待态；若之前被打回过，展示上一次打回理由供参考
- 超时48小时未提交 → 视为"拒绝比赛"终止，订场人记未响应次数

### 3. SCHEDULED：确认赛约
- 非订场人（组内其他人）看到赛约信息，三个操作：[接受] / [打回重订，选理由] / [拒绝比赛，选理由]
- 全部接受 → 创建 Meetup（`matchId` 关联，`meetupType=TOURNAMENT`），流转到"待比赛"
- 打回重订 → 不终止，退回 `BOOKING`，所有人 `confirmStatus` 重置，不计入拒绝次数，不设次数上限
- 拒绝比赛 → 终止，`REJECTED`，拒绝方记拒绝次数（若已达上限，则该人下次只能点"接受"，没有"拒绝比赛"选项，仍可"打回重订"）
- 48小时未操作 → 默认接受

### 4. 待比赛 → PENDING_CONFIRM：提交结果
- 任意一人先操作："谁赢了？" 单选参与者/队伍列表（2人、3人或双打按队选）→ [提交]，不填比分
- 其余人逐个操作：[确认] / [拒绝结果，选理由]（达到拒绝上限则只有确认）
- 全部确认 → COMPLETED，`winnerId` 写入，胜者晋级；败者 ELIMINATED
- 拒绝结果 → 终止，`REJECTED`，全员回匹配池，重新等待下次凌晨匹配，拒绝方记拒绝次数
- 48小时未操作 → 默认同意

---

## 五、资格赛 → 正赛流转

- 赢一场（或组内晋级）即获得正赛资格，弹出支付引导
- 支付无超时限制，随时可创建订单
- 支付成功 → 锁定席位，`currentFilledSlots +1`
- 未支付 → 资格保留但不占用席位，其他人继续资格赛
- 席位满（`currentFilledSlots = totalSlots`）→ 资格赛停止，未支付者失去资格
- 资格赛截止时间到达仍未满 → 若设置了 `qualifierEndTime`，届时停止；未设置则永久开放直到满员

### 支付流程（小程序侧）

1. 用户在落地页点击"立即支付"，调用 `POST /tournament/entry/pay`
2. 后端创建支付订单，调用 `PaymentDomainService` 下单，返回 `PrepayResult`（`prepayId`/`timeStamp`/`nonceStr`/`packageVal`/`signType`/`paySign`），字段对齐 `wx.requestPayment` 入参
3. 前端拿到 `PrepayResult` 直接调用小程序 `wx.requestPayment` 拉起支付面板，无需再单独处理金额，金额已在下单时与订单绑定
4. 支付结果由微信异步回调通知后端（复用现有支付回调链路），回调成功后置 `TournamentEntryPO.status = MAIN_DRAW_WAITING_MATCH`，`paidTime` 写入，`currentFilledSlots + 1`
5. 前端不依赖支付成功的同步返回值判断结果，落地页轮询或依赖下次进入时的 `detail` 接口刷新状态

### 退出赛事

- 资格赛阶段（`status` 为 `QUALIFIER_WAITING_MATCH`/`QUALIFIER_IN_MATCH`）：直接退出，无费用，`status` 置为已退出，不进入 `ELIMINATED`（区分主动退出与被淘汰，便于时间线展示）
- 正赛阶段（已支付，`status` 为 `AWAIT_PAYMENT` 之后）：退出需先触发退款，退款成功后释放席位（`currentFilledSlots -1`），再置退出状态
- **退款需新增能力**：现有支付体系（`WechatPayClient`）仅支持下单、查单、关单、分账，没有退款接口，需要新增微信支付退款 API 封装 + 领域层退款流程，MVP 阶段需要评估是否纳入首版范围

---

## 六、正赛重新匹配

- 每轮同样走凌晨2点批量匹配
- 拒绝产生的双方都回到本轮匹配池
- 匹配时优先避开互相拒绝过的组合，除非无法凑齐（如只剩2人）才强制匹配
- 指定轮次（`offlineFromRound`）之后转线下，由平台负责场地，选手不需要经过订场流程

---

## 七、信用体系（仅记录，不处罚，MVP 阶段）

- 拒绝次数（终止性的"拒绝比赛"/"拒绝结果"，不含"打回重订"）：达到赛事配置的上限后，该用户在对应阶段（资格赛/正赛）只能点"确认"或"打回重订"，不能再点"拒绝"
- 未响应次数：只记录，不做限制
- **展示措辞轻量化**：这套机制刻意允许一次"不服就能申诉重来"，本质是给用户的正当机会而不是失信行为，落地页展示时不用负面表述（如"XX拒绝确认2次"），改用中性/轻松的说法，例如"XX申诉了1次"；"打回重订"更是常规协商操作，不在信用记录中出现
- 展示位置：MVP 阶段展示在比赛落地页的"信用记录"区块；个人主页展示留给 V2

---

## 八、比赛落地页（收据式长页面）

单页面承载赛事全部交互与历史，操作不做成固定吸底栏，而是放在页面第一屏可见的"当前待办卡片"里，卡片内容随 `actionState` 变化，操作按钮跟当前状态的上下文信息（如对手、赛约信息）绑定展示；再往下是完整时间线供翻阅，不需要操作时不占用视觉焦点。

按状态区分三种视角：

- **未报名**：展示赛事信息、正赛席位进度条、报名费、[立即报名]
- **已报名进行中**：当前待办卡片展示状态（资格赛中/正赛N强）+ 对手信息 + 对应 `actionState` 的操作按钮（订场/接受/打回重订/拒绝比赛/提交结果/确认结果/支付）
- **已被淘汰**：展示战绩汇总、[查看完整赛程]

页面固定包含：
1. 顶部：赛事名称、海报、正赛席位进度条
2. 当前待办卡片：第一屏可见，展示当前状态、对手/赛约信息，操作按钮直接嵌在卡片内
3. 赛事时间线：从报名开始，逐条记录所有操作事件（匹配成功、订场、赛约确认、打回重订、约球完成、结果提交/确认、支付、晋级、申诉等），时间倒序或正序展示，类似物流轨迹
4. 签表（可展开）：查看完整对阵图
5. 信用记录：展示本赛事内各选手的申诉次数（措辞轻量化，见第七章）

---

## 九、MVP 范围确认

### 包含
- 赛事创建与配置（草稿/激活/废弃三态）
- 报名池，报名信息可随时修改
- 定时批量匹配（凌晨2点），领域层实现匹配算法
- 资格赛支持分组人数配置（2人或3人组，3人组主观投票选晋级者，不算比分）
- 比赛完整状态机（订场人选择、订场、赛约确认、结果提交确认），全部操作收口在比赛落地页
- 约球活动关联比赛（`matchId` + `meetupType`），约球流程与比赛解耦
- 支付流程锁定正赛席位
- 正赛逐轮匹配，避让互相拒绝的组合
- 指定轮次转线下
- 比赛落地页（收据式时间线 + 信用记录展示）

### 暂不包含（V2 及以后）
- 循环赛计分/仲裁机制
- 作弊检测
- AI 自动仲裁
- Lucky Loser 替补机制
- 详细战绩统计、生涯数据
- NTRP 动态评级
- 自动推荐公共球场
- 个人主页信用展示

---

## 十、接口设计

### 设计原则

赛事落地页收口为**一个详情接口**，靠 `actionState` 驱动"当前待办卡片"内的操作按钮渲染；比赛的订场、赛约确认、打回重订、拒绝、结果提交确认等全部操作都由落地页的当前待办卡片触发，不跳转到独立页面。

### 1. 赛事管理（运营后台）

| 接口 | 说明 |
|------|------|
| `POST /tournament/admin/create` | 创建赛事（草稿） |
| `POST /tournament/admin/update` | 编辑赛事草稿 |
| `POST /tournament/admin/activate` | 激活赛事 |
| `POST /tournament/admin/abandon` | 废弃赛事 |
| `POST /tournament/admin/list` | 后台赛事列表 |

### 2. 赛事浏览与报名（用户端）

赛事仅通过 banner 对外暴露入口，不提供赛事列表页，用户直接从 banner 跳转到指定赛事的落地页。

| 接口 | 说明 |
|------|------|
| `GET /tournament/detail/{bizId}` | 赛事详情（核心接口，结构见下） |
| `POST /tournament/entry/join` | 报名 |
| `POST /tournament/entry/update` | 修改报名偏好（区域/场地能力/可比赛时间） |
| `POST /tournament/entry/pay` | 支付报名费下单，返回 `PrepayResult` 供小程序拉起支付 |
| `POST /tournament/entry/withdraw` | 退出赛事，资格赛阶段直接退出；正赛阶段退出触发退款 |

### 3. 比赛操作（均由落地页当前待办卡片触发）

| 接口 | 说明 |
|------|------|
| `POST /tournament/match/court-booker` | 选择/放弃订场人身份（乐观锁防止双方同时抢订场人身份） |
| `POST /tournament/match/book` | 提交赛约（场地+时间） |
| `POST /tournament/match/schedule-confirm` | 处理赛约：接受 / 打回重订（`rebookReasonCode`+可选文本） / 拒绝比赛（`rejectReasonCode`+可选文本） |
| `POST /tournament/match/submit-result` | 提交比赛结果（选谁赢了/哪个队伍赢了） |
| `POST /tournament/match/result-confirm` | 处理结果：确认 / 拒绝结果（`rejectReasonCode`+可选文本） |

### 4. 定时任务（内部）

| 任务 | 说明 |
|------|------|
| 每日凌晨2点批量匹配 Job | 只在 `qualifierStartTime` 之后触发 |

### 5. `GET /tournament/detail/{bizId}` 结构详解

```text
{
  tournament: {                        // 赛事基础信息
    bizId, tournamentName, posterKey, cityName, ntrpLevel, genderLimit,
    entryFee, registrationStartTime, registrationEndTime,
    qualifierStartTime, qualifierEndTime, offlineFromRound, status
  },

  progress: {                          // 公开赛事进程，所有访问者可见
    currentFilledSlots, totalSlots,     // 正赛席位进度，如 18/32
    currentRound,                       // 当前公开进行的轮次
    currentRoundTotalMatches,           // 本轮总比赛场数
    currentRoundCompletedMatches,       // 本轮已完成场数
    registrationEndTime,
    qualifierEndTime
  },

  myEntry: {...} | null,               // 当前用户报名信息，未报名为 null

  myCurrentMatch: {                    // 当前用户进行中的比赛，无则为 null
    matchId, round, opponents: [...],
    courtBookerId, courtName, courtAddress,
    scheduledStartTime, scheduledEndTime,
    meetupId, status, participants: [...]
  } | null,

  actionState: "NOT_REGISTERED"        // 决定"当前待办卡片"渲染内容的显式状态
                                        // | AWAIT_PAYMENT
                                        // | AWAIT_COURT_BOOKER_SELECT
                                        // | AWAIT_BOOKING（我是订场人，待提交场地时间）
                                        // | AWAIT_BOOKING_OPPONENT（对方订场中，若之前被打回展示打回理由）
                                        // | AWAIT_SCHEDULE_CONFIRM（待我接受/打回重订/拒绝比赛）
                                        // | AWAIT_RESULT_SUBMIT（待提交谁赢了）
                                        // | AWAIT_RESULT_CONFIRM（待我确认结果/拒绝结果）
                                        // | WAITING_MATCH（排队等待下次匹配）
                                        // | ELIMINATED
                                        // | QUALIFIED_MAIN_DRAW（正赛进行中，无待办）

  myTimeline: [...],                   // 仅个人视角事件流，不含其他人操作
  bracket: {...},                      // 签表数据
  myCreditRecords: [...]               // 仅展示与我相关的申诉/未响应记录，措辞轻量化
}
```

`actionState` 由后端根据 `myEntry.status` + `myCurrentMatch.status` + 是否为订场人 + 各方确认状态一次性计算返回，前端只需 switch-case 渲染当前待办卡片，不需要自行拼装状态判断逻辑。
