# 模块 1：赛事管理（运营后台）

## 职责
运营人员创建、编辑、激活、废弃赛事，管理赛事生命周期的 DRAFT → ACTIVE → ABANDONED 三态。所有配置项（签位数、转线下轮次、报名费、各时间点、拒绝上限、资格赛组人数）在此模块设定。

## 聚合根 / 领域对象
- **Tournament（聚合根）**：本模块是它的创建与配置入口。
  - 关键字段：tournamentName、posterKey、cityCode/cityName、ntrpLevel、genderLimit、totalSlots、offlineFromRound、qualifierGroupSize、entryFee、报名/资格赛各时间点、qualifierRejectLimit、mainDrawRejectLimit、status、currentFilledSlots。
  - 状态机：DRAFT（可编辑）→ ACTIVE（对外可见、可报名）→ ABANDONED（终止）。
  - 不变量：仅 DRAFT 可编辑；激活时校验必填配置齐全且时间点先后合理（报名开始 < 资格赛开始）；ABANDONED 不可逆。

## 领域 Service 能力（TournamentAdminService）
- `create(配置)`：生成 bizId（IdWorker），落 DRAFT。
- `update(bizId, 配置)`：仅 DRAFT 可改，校验后覆盖配置。
- `activate(bizId)`：校验配置完整性与时间点合法性，DRAFT → ACTIVE。
- `abandon(bizId)`：任意非终止态 → ABANDONED。
- `pageList(查询条件)`：后台分页列表。
- 校验统一走 `Assert` + `BizErrorCode`。

## 接口清单

### `POST /tournament/admin/create`
创建赛事草稿。入参 `TournamentCreateCmd`（全部配置项）。app 层用 mapstruct 转领域配置对象 → 调 `create` → 返回 bizId 的 DTO。校验：签位数枚举合法（16/32/64）、offlineFromRound < totalSlots、entryFee ≥ 0、时间点先后。

### `POST /tournament/admin/update`
编辑草稿。入参 `TournamentUpdateCmd`（bizId + 可改配置）。仅当 status=DRAFT 才允许，否则 Assert 抛错。

### `POST /tournament/admin/activate`
激活。入参含 bizId。领域层做完整性校验后置 ACTIVE。激活后报名池按 registrationStartTime 开放。

### `POST /tournament/admin/abandon`
废弃。入参含 bizId + 可选原因。置 ABANDONED，赛事对用户端不再展示入口。

### `POST /tournament/admin/list`
后台赛事列表。入参分页 + 过滤（城市、状态、NTRP）。返回 `TournamentAdminItemDTO` 列表，含席位进度、当前轮次概览。

## 与其他模块的边界
- 只负责赛事本身的配置与状态；席位计数的增减（currentFilledSlots）由模块 5 在支付/退款时驱动，本模块只读展示。
- 激活后，报名池开放的判断由模块 2 依据 registrationStartTime 做。
