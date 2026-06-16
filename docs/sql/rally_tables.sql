-- Rally 约球系统建表语句
-- 包含：配置域 sys_config、用户域 user_tennis_profile / user_profile_change_log

-- ============================================================
-- 1. 配置域：全局配置表
-- ============================================================

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`       VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `config_key`   VARCHAR(128) NOT NULL COMMENT '配置键，规范 domain.module.field，如 score.calibration.deviation_t1',
  `config_value` VARCHAR(2048) NOT NULL COMMENT '字符串化值；json 类型存序列化串',
  `value_type`   varchar(8) NOT NULL DEFAULT 'string' COMMENT '值类型，决定读取时的解析方式',
  `scope`        VARCHAR(64)  NOT NULL DEFAULT 'global' COMMENT '作用域：global 或 city:{cityCode} 等',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '中文说明',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用，0 停用（停用则读取回退默认值）',
  `version`      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，便于变更日志与并发写',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_key_scope` (`config_key`, `scope`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局配置表';

-- ============================================================
-- 2. 用户域：球员网球档案表
-- ============================================================

DROP TABLE IF EXISTS `user_tennis_profile`;
CREATE TABLE `user_tennis_profile` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`            VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `user_id`           VARCHAR(32)  NOT NULL COMMENT '关联 users.user_id',
  `videos`            JSON         DEFAULT NULL COMMENT '打球视频列表 [{key,title}]，存储放宽最多 5，交互上限走配置 user.video.max_count 默认 3（裁定 D1）',
  `ntrp_score`        DECIMAL(3,1) DEFAULT NULL COMMENT 'NTRP 自评 1.5~7.0 步长 0.5',
  `utr_score`         DECIMAL(4,2) DEFAULT NULL COMMENT 'UTR 三方接入选填，MVP 预留不实现',
  `ntrp_updated_at`   DATETIME     DEFAULT NULL COMMENT 'NTRP 最后修改时间，冷却期计算基准',
  `status`            VARCHAR(16) NOT NULL DEFAULT 'tbc' COMMENT '档案状态（裁定 D2）：tbc 未填写 / normal 正常 / under_review 核查期',
  `reputation_score`  DECIMAL(5,2) NOT NULL DEFAULT 100 COMMENT '信誉分，默认 100，由评分域写入（spec-04）',
  `credibility_score` DECIMAL(5,2) NOT NULL DEFAULT 0  COMMENT '可信度，由评分域写入（spec-04）',
  `calibration_score` DECIMAL(5,2) NOT NULL DEFAULT 80 COMMENT '校准度，票数不足默认 80（裁定 D8），由评分域写入',

  `is_under_review`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否核查期，与 status=under_review 同步冗余，便于查询',
  `review_remaining_matches` INT        DEFAULT NULL COMMENT '核查期剩余需完成场次，触发核查期时写入，每完成一场减 1，归零后解除',
  `is_newbie`              TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '新人角标，收到 >=3 次评价后置 0（score.newbie.min_reviews）',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员网球档案表';

-- ============================================================
-- 3. 用户域：用户档案变更日志
-- ============================================================

DROP TABLE IF EXISTS `user_profile_change_log`;
CREATE TABLE `user_profile_change_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`       VARCHAR(32)  NOT NULL COMMENT '雪花 ID',
  `user_id`      VARCHAR(32)  NOT NULL COMMENT '用户',
  `type`         VARCHAR(16) NOT NULL COMMENT '变更类型',
  `before_value` DECIMAL(6,2) DEFAULT NULL COMMENT '变更前值；under_review 进入时存锁定场数(required_matches)',
  `after_value`  DECIMAL(6,2) DEFAULT NULL COMMENT '变更后值；under_review 进度推进时存剩余场数',
  `value`        DECIMAL(6,2) DEFAULT NULL COMMENT '变更量（+向上/-向下）',
  `reason`       VARCHAR(32)  NOT NULL COMMENT '原因枚举：user 手动 / system 系统 / system_suggest 建议免核查 / review_bad 遇差票 等',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `ref_id`       VARCHAR(32)  DEFAULT NULL COMMENT '关联业务 biz_id（如 meetup_id / review_id），可空',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_idempotent` (`user_id`, `type`, `ref_id`, `reason`),
  KEY `idx_user_type` (`user_id`, `type`),
  KEY `idx_ref` (`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户档案变更日志';


-- ============================================================
-- 5. 约球域：约球主表
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup`;
CREATE TABLE `rally_meetup` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32)  NOT NULL COMMENT '业务主键（雪花算法字符串）',
  `creator_id`      VARCHAR(32)  NOT NULL COMMENT '发布者，关联 users.user_id',
  `title`           VARCHAR(128) DEFAULT NULL COMMENT '标题，选填；不填后端按模板生成',
  `match_type`      varchar(8) NOT NULL DEFAULT 'single' COMMENT '类型：单打/双打/拉球',
  `max_players`     INT          NOT NULL COMMENT '人数上限',
  `current_players` INT          NOT NULL DEFAULT 1 COMMENT '已加入人数（含发布者，发布即 1）',
  `city_code`       VARCHAR(32)  NOT NULL COMMENT '城市编码，由 court_location 反查后端写入',
  `city_name`       VARCHAR(64)  NOT NULL COMMENT '城市名称',
  `district_name`   VARCHAR(64)  DEFAULT NULL COMMENT '区域名称，选填',
  `start_time`      DATETIME     NOT NULL COMMENT '活动开始时间（含日期，+08:00）',
  `end_time`        DATETIME     NOT NULL COMMENT '结束时间 = start_time + duration 小时，懒判定/兜底查询用（裁定 D3/D4）',
  `duration`        DECIMAL(3,1) NOT NULL COMMENT '持续小时：0.5/1.0/1.5/2.0/2.5/3.0',
  `court_name`      VARCHAR(128) DEFAULT NULL COMMENT '场地名称，手填',
  `court_address`   VARCHAR(256) NOT NULL COMMENT '场地详细地址，手填或地图点选',
  `court_lng`       DOUBLE       NOT NULL COMMENT '场地经度',
  `court_lat`       DOUBLE       NOT NULL COMMENT '场地纬度',

  `level_mode`      varchar(8) DEFAULT 'exact' COMMENT '水平要求模式',
  `level_min`       DECIMAL(3,1) DEFAULT NULL COMMENT '水平最小值，RANGE/EXACT/ABOVE 必填',
  `level_max`       DECIMAL(3,1) DEFAULT NULL COMMENT '水平最大值，RANGE/EXACT/BELOW 必填',
  `gender_limit`    varchar(8) NOT NULL DEFAULT 'any' COMMENT '性别限制',
  `join_mode`       varchar(8) NOT NULL DEFAULT 'direct' COMMENT '加入模式：直接/审批',
  `cost_items`      JSON         DEFAULT NULL COMMENT '费用明细 [{name,totalAmount(分)}]，纯展示',
  `status`          varchar(16) NOT NULL DEFAULT 'open' COMMENT '状态机',
  `court_index`     VARCHAR(64)  DEFAULT NULL COMMENT '场地索引，前端透传存储',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_city_status_end` (`city_code`, `status`, `end_time`) COMMENT '列表懒判定过滤主索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约球主表';

-- ============================================================
-- 6. 约球域：报名/审核等待表
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_registration`;
CREATE TABLE `rally_meetup_registration` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务主键（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `user_id`         VARCHAR(32) NOT NULL COMMENT '参与人（含创建者），关联 users.user_id',
  `status`          varchar(16) NOT NULL DEFAULT 'pending' COMMENT '报名状态机：pending/approved/rejected/expired/withdrawn',
  `expires_at`      DATETIME    DEFAULT NULL COMMENT '自动撤回失效时间，NULL=不自动撤回',
  `opt_time`        DATETIME    DEFAULT NULL COMMENT '管理人审批操作时间',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_user_status` (`user_id`, `status`) COMMENT '查我的报名 + 冲突检测',
  KEY `idx_meetup_status` (`rally_meetup_id`, `status`) COMMENT '审批列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约球报名/注册表（记录所有参与者：创建者、等待审批、已通过等）';

-- ============================================================
-- 7. 评价域：评价竖表
-- ============================================================

DROP TABLE IF EXISTS `rally_review`;
CREATE TABLE `rally_review` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `from_user_id`    VARCHAR(32) NOT NULL COMMENT '评价人 user_id（仅本人，不可代评）',
  `to_user_id`      VARCHAR(32) NOT NULL COMMENT '被评价人 user_id',
  `review_type`     VARCHAR(16) NOT NULL COMMENT '评价维度：ntrp_vote 水平三元投票 / attendance 出勤 / tag 个性化标签',
  `review_value`    VARCHAR(512) NOT NULL COMMENT '评价值。ntrp_vote: higher/same/lower；attendance: on_time/late/no_show；tag: 逗号分隔的多标签',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  -- 每场每人对另一人的每个维度只有一行：存在则更新（tag 整串逗号分隔），不删旧
  UNIQUE KEY `uk_review_dim` (`rally_meetup_id`,`from_user_id`,`to_user_id`,`review_type`),
  KEY `idx_to_user` (`to_user_id`,`review_type`) COMMENT '球员主页按被评价人+维度聚合',
  KEY `idx_meetup_from` (`rally_meetup_id`,`from_user_id`) COMMENT '查我在某场已评状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价竖表（NTRP三元投票/出勤/个性化标签，每维度一行，tag 逗号分隔）';

-- ============================================================
-- 8. 评价域：比分记录表（按盘）
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_score`;
CREATE TABLE `rally_meetup_score` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `set_number`      INT         NOT NULL COMMENT '第几盘，从 1 开始',
  `set_format`      VARCHAR(16) NOT NULL COMMENT '赛制：GAME / TIEBREAK',
  `match_type`      VARCHAR(16) NOT NULL DEFAULT '' COMMENT '比赛类型：SINGLE / DOUBLE / RALLY',
  `meetup_date`     DATETIME    DEFAULT NULL COMMENT '比赛日期（冗余自 rally_meetup.start_time）',
  `venue_name`      VARCHAR(256) DEFAULT '' COMMENT '比赛场地名称（冗余自 rally_meetup.court_name）',
  `side_a_player1`  VARCHAR(32) NOT NULL COMMENT 'A 侧选手1 user_id（裁定 D13，单个）',
  `side_a_player1_nickname` VARCHAR(128) DEFAULT '' COMMENT 'A 侧选手1昵称（冗余存储）',
  `side_a_player1_avatar` VARCHAR(512) DEFAULT '' COMMENT 'A 侧选手1头像URL（冗余存储）',
  `side_a_player2`  VARCHAR(32) DEFAULT NULL COMMENT 'A 侧选手2 user_id，单打为 NULL',
  `side_a_player2_nickname` VARCHAR(128) DEFAULT '' COMMENT 'A 侧选手2昵称（冗余存储）',
  `side_a_player2_avatar` VARCHAR(512) DEFAULT '' COMMENT 'A 侧选手2头像URL（冗余存储）',
  `side_b_player1`  VARCHAR(32) NOT NULL COMMENT 'B 侧选手1 user_id',
  `side_b_player1_nickname` VARCHAR(128) DEFAULT '' COMMENT 'B 侧选手1昵称（冗余存储）',
  `side_b_player1_avatar` VARCHAR(512) DEFAULT '' COMMENT 'B 侧选手1头像URL（冗余存储）',
  `side_b_player2`  VARCHAR(32) DEFAULT NULL COMMENT 'B 侧选手2 user_id，单打为 NULL',
  `side_b_player2_nickname` VARCHAR(128) DEFAULT '' COMMENT 'B 侧选手2昵称（冗余存储）',
  `side_b_player2_avatar` VARCHAR(512) DEFAULT '' COMMENT 'B 侧选手2头像URL（冗余存储）',
  `side_a_score`    INT         NOT NULL COMMENT 'A 侧本盘比分（局数/抢七分）',
  `side_b_score`    INT         NOT NULL COMMENT 'B 侧本盘比分',
  `side_a_tiebreak_score` INT   DEFAULT NULL COMMENT 'A 侧抢七比分（本盘 6:6 时记录）',
  `side_b_tiebreak_score` INT   DEFAULT NULL COMMENT 'B 侧抢七比分（本盘 6:6 时记录）',
  `recorded_by`     VARCHAR(32) NOT NULL COMMENT '记录人 user_id（任意参与者可代记）',
  `version`         INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_meetup_set` (`rally_meetup_id`,`set_number`) COMMENT '同场同盘唯一',
  KEY `idx_meetup` (`rally_meetup_id`) COMMENT '按场查全部盘'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比分记录表（按盘，双侧选手，乐观锁）';

-- ============================================================
-- 9. 评价域：追加配置项
-- ============================================================

INSERT INTO `sys_config` (`biz_id`, `config_key`, `config_value`, `value_type`, `scope`, `description`, `enabled`, `version`) VALUES
('cfg0000000000000064', 'review.tag.max_length', '8', 'int', 'global', '手动输入单标签字符上限', 1, 0),
('cfg0000000000000065', 'review.tag.max_custom_per_review', '3', 'int', 'global', '单次评价手动输入标签数上限', 1, 0),
('cfg0000000000000066', 'review.score.games4_max', '5', 'int', 'global', '4局制单侧最大局数', 1, 0),
('cfg0000000000000067', 'review.score.games6_max', '7', 'int', 'global', '6局制单侧最大局数', 1, 0),
('cfg0000000000000068', 'review.score.tiebreak_min', '7', 'int', 'global', '抢七胜方最低分', 1, 0),
('cfg0000000000000069', 'review.score.tiebreak_lead', '2', 'int', 'global', '抢七最低领先分', 1, 0);

-- ============================================================
-- 10. 评分域：批量评分幂等状态表
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_score_status`;
CREATE TABLE `rally_meetup_score_status` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`            VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `meetup_id`         VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id（唯一）',
  `score_version`     INT         NOT NULL DEFAULT 0 COMMENT '重算版本：评价/比分变更时 +1，processed_version 落后则需重算',
  `processed_version` INT         NOT NULL DEFAULT -1 COMMENT '已处理到的版本号，初始 -1（从未处理）',
  `processed_at`      DATETIME    DEFAULT NULL COMMENT '最近一次处理完成时间，NULL=从未处理',
  `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_meetup_id` (`meetup_id`),
  KEY `idx_pending` (`processed_at`) COMMENT '扫描待处理（processed_at IS NULL 或 version 落后）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量评分幂等状态表（score_version 重算控制）';

-- ============================================================
-- 11. 场地域：球场信息表
-- ============================================================

DROP TABLE IF EXISTS `rally_court`;
CREATE TABLE `rally_court` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`         VARCHAR(32)  NOT NULL COMMENT '业务主键（雪花算法字符串）',
  `name`           VARCHAR(128) NOT NULL COMMENT '球场名称',
  `address`        VARCHAR(256) NOT NULL COMMENT '球场详细地址',
  `lng`            DOUBLE       NOT NULL COMMENT '球场经度',
  `lat`            DOUBLE       NOT NULL COMMENT '球场纬度',
  `city_code`      VARCHAR(32)  NOT NULL COMMENT '城市编码',
  `district_code`  VARCHAR(32)  DEFAULT NULL COMMENT '区域编码，选填',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_city_district` (`city_code`, `district_code`) COMMENT '按城市/区域查询球场'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球场信息表';

-- ============================================================
-- 12. 约球域：活动群聊消息表
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_chat_message`;
CREATE TABLE `rally_meetup_chat_message` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32)  NOT NULL COMMENT '消息业务主键（雪花ID）',
  `meetup_id`       VARCHAR(32)  NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `sender_id`       VARCHAR(32)  NOT NULL COMMENT '发送者 user_id',
  `sender_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发送者昵称（冗余存储）',
  `sender_avatar`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '发送者头像URL（冗余存储）',
  `content`         TEXT NOT NULL COMMENT '消息内容（文本/图片URL/表情标识）',
  `content_type`    VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本/IMAGE-图片/LOCATION-位置',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_meetup_biz` (`meetup_id`, `biz_id`) COMMENT '按活动+bizId游标拉取消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动群聊消息表';

-- ============================================================
-- 13. 约球域：活动群聊用户表（含已读状态）
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_chat_user`;
CREATE TABLE `rally_meetup_chat_user` (
  `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`              VARCHAR(32) NOT NULL COMMENT '业务主键（雪花ID）',
  `meetup_id`           VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `user_id`             VARCHAR(32) NOT NULL COMMENT '用户 user_id',
  `last_read_message_id` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '已读最新消息bizId（仅用于未读数计算）',
  `unread_count`        INT         NOT NULL DEFAULT 0 COMMENT '未读消息数（冗余存储）',
  `joined_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入聊天时间',
  `create_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_meetup_user` (`meetup_id`, `user_id`) COMMENT '每个用户在每个活动只有一条记录',
  KEY `idx_meetup` (`meetup_id`) COMMENT '查询活动的所有参与者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动群聊用户表（含已读状态）';

-- ============================================================
-- 14. 用户域：用户关注关系表
-- ============================================================

DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`        VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `follower_id`   VARCHAR(32)  NOT NULL COMMENT '关注人 user_id',
  `following_id`  VARCHAR(32)  NOT NULL COMMENT '被关注人 user_id',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_follow_rel` (`follower_id`, `following_id`) COMMENT '同一关注关系唯一',
  KEY `idx_following` (`following_id`) COMMENT '查被关注（粉丝）列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';
