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
  `video_urls`        JSON         DEFAULT NULL COMMENT '打球视频 key 列表，存储放宽最多 5，交互上限走配置 user.video.max_count 默认 3（裁定 D1）',
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
-- 4. 配置域：63 项配置键初始化数据
-- ============================================================

INSERT INTO `sys_config` (`biz_id`, `config_key`, `config_value`, `value_type`, `scope`, `description`, `enabled`, `version`) VALUES
-- 5.1 用户域（user）5 项
('cfg0000000000000001', 'user.video.max_count', '3', 'int', 'global', '视频最多上传个数（裁定 D1，存储放宽至 5）', 1, 0),
('cfg0000000000000002', 'user.video.max_size_mb', '5', 'int', 'global', '单视频大小上限（MB），超限前端直接拒绝', 1, 0),
('cfg0000000000000003', 'user.video.max_duration_sec', '60', 'int', 'global', '单视频时长上限（秒），约 1 分钟', 1, 0),
('cfg0000000000000004', 'user.ntrp.intro_text', 'NTRP 是国际通用的网球水平自评标准（1.5–7.0）。1.5 为初学者，3.0 可稳定多拍对打，4.5 具备进攻与战术，6.0+ 为职业水平。请如实选择，便于匹配到水平相近的球友。', 'string', 'global', 'NTRP 自评介绍文案，onboarding 滚轮旁提示', 1, 0),
('cfg0000000000000005', 'user.video.upload_tip', '上传一段你的打球视频（约 1 分钟），让球友快速了解你的水平与风格，约球邀约更容易被接受哦。', 'string', 'global', '视频上传引导文案，帮球友快速了解你', 1, 0),

-- 5.2 评分域 · 权重与等级（score.weights / score.rating / score.newbie）7 项
('cfg0000000000000006', 'score.weights.reputation', '0.5', 'float', 'global', '总分权重 W1（信誉分）', 1, 0),
('cfg0000000000000007', 'score.weights.credibility', '0.3', 'float', 'global', '总分权重 W2（可信度）', 1, 0),
('cfg0000000000000008', 'score.weights.calibration', '0.2', 'float', 'global', '总分权重 W3（校准度）', 1, 0),
('cfg0000000000000009', 'score.rating.s_threshold', '90', 'int', 'global', 'S 等级下限（总分 ≥90）', 1, 0),
('cfg0000000000000010', 'score.rating.a_threshold', '75', 'int', 'global', 'A 等级下限（75–89）', 1, 0),
('cfg0000000000000011', 'score.rating.b_threshold', '55', 'int', 'global', 'B 等级下限（55–74）；<55 为 C', 1, 0),
('cfg0000000000000012', 'score.newbie.min_reviews', '3', 'int', 'global', '新人转入实时计算所需评价数（裁定 D14）', 1, 0),

-- 5.3 评分域 · 信誉分（score.reputation）4 项
('cfg0000000000000013', 'score.reputation.on_time', '2', 'int', 'global', '被标「准时」或未被标记 +2（裁定 D6）', 1, 0),
('cfg0000000000000014', 'score.reputation.late', '-10', 'int', 'global', '被标「迟到」扣分', 1, 0),
('cfg0000000000000015', 'score.reputation.no_show', '-25', 'int', 'global', '被标「爽约」扣分', 1, 0),
('cfg0000000000000016', 'score.reputation.min', '0', 'int', 'global', '信誉分下限，不为负', 1, 0),

-- 5.4 约球域 · 取消/退出阶梯扣分（meetup.cancel / meetup.quit）8 项
('cfg0000000000000017', 'meetup.cancel.penalty_24h_out', '5', 'int', 'global', '发布者取消（有人报名）24h 外扣分', 1, 0),
('cfg0000000000000018', 'meetup.cancel.penalty_12_24h', '10', 'int', 'global', '取消 12–24h 扣分', 1, 0),
('cfg0000000000000019', 'meetup.cancel.penalty_6_12h', '15', 'int', 'global', '取消 6–12h 扣分', 1, 0),
('cfg0000000000000020', 'meetup.cancel.penalty_under_6h', '25', 'int', 'global', '取消 <6h 扣分', 1, 0),
('cfg0000000000000021', 'meetup.quit.penalty_6h_out', '0', 'int', 'global', '报名者 6h 外退出（释放位置，不扣分）', 1, 0),
('cfg0000000000000022', 'meetup.quit.penalty_under_6h', '25', 'int', 'global', '报名者 <6h 退出（视为爽约）扣分', 1, 0),
('cfg0000000000000023', 'meetup.quit.threshold_hours', '6', 'int', 'global', '退出扣分时间分界（小时），<该值算爽约', 1, 0),
('cfg0000000000000024', 'meetup.edit_lock_minutes_before_start', '60', 'int', 'global', '编辑锁定提前量（分钟）', 1, 0),

-- 5.5 评分域 · 可信度（score.credibility）6 项
('cfg0000000000000025', 'score.credibility.match_per_score', '6', 'int', 'global', '近窗口内单场完成约球加分', 1, 0),
('cfg0000000000000026', 'score.credibility.match_score_cap', '60', 'int', 'global', '约球项加分上限', 1, 0),
('cfg0000000000000027', 'score.credibility.video_per_score', '5', 'int', 'global', '每个打球视频加分（裁定 D7）', 1, 0),
('cfg0000000000000028', 'score.credibility.video_cap', '25', 'int', 'global', '视频项加分上限（裁定 D7）', 1, 0),
('cfg0000000000000029', 'score.credibility.utr_score', '15', 'int', 'global', 'UTR 绑定加分（裁定 D7，单次封顶即 15）', 1, 0),
('cfg0000000000000030', 'score.credibility.match_window_days', '90', 'int', 'global', '完成约球统计窗口（天），近 90 天', 1, 0),

-- 5.6 评分域 · 校准度（score.calibration）12 项
('cfg0000000000000031', 'score.calibration.min_votes', '10', 'int', 'global', '最低有效票数', 1, 0),
('cfg0000000000000032', 'score.calibration.deviation_t1', '0.20', 'float', 'global', '一档偏差阈值（20%）', 1, 0),
('cfg0000000000000033', 'score.calibration.deviation_t2', '0.50', 'float', 'global', '二档偏差阈值（50%）', 1, 0),
('cfg0000000000000034', 'score.calibration.score_under_t1', '100', 'int', 'global', '偏差 <t1 给分', 1, 0),
('cfg0000000000000035', 'score.calibration.score_below_t1_t2', '75', 'int', 'global', '偏低 t1–t2 给分', 1, 0),
('cfg0000000000000036', 'score.calibration.score_above_t1_t2', '50', 'int', 'global', '偏高 t1–t2 给分', 1, 0),
('cfg0000000000000037', 'score.calibration.score_below_t2', '55', 'int', 'global', '偏低 >t2 给分', 1, 0),
('cfg0000000000000038', 'score.calibration.score_above_t2', '20', 'int', 'global', '偏高 >t2 给分', 1, 0),
('cfg0000000000000039', 'score.calibration.score_insufficient', '80', 'int', 'global', '票数不足给分（裁定 D8）', 1, 0),
('cfg0000000000000040', 'score.calibration.suggest_min_votes', '8', 'int', 'global', '触发自评建议最低票数', 1, 0),
('cfg0000000000000041', 'score.calibration.suggest_deviation', '0.25', 'float', 'global', '触发建议偏差阈值（25%）', 1, 0),
('cfg0000000000000042', 'score.calibration.suggest_concentration', '0.60', 'float', 'global', '触发建议同向票占比阈值（60%）', 1, 0),

-- 5.7 评分域 · 核查期与 NTRP 冷却（score.review_period / score.ntrp）6 项
('cfg0000000000000043', 'score.review_period.required_matches', '3', 'int', 'global', '核查期解除所需完成场次', 1, 0),
('cfg0000000000000044', 'score.review_period.penalty_credibility', '50', 'int', 'global', '核查期遇「差」暂降可信度（裁定 D10，冻结为该值）', 1, 0),
('cfg0000000000000045', 'score.review_period.trigger_ntrp_delta', '0.5', 'float', 'global', '触发核查期的自评向上修改幅度（≥0.5 级，向下不触发）', 1, 0),
('cfg0000000000000046', 'score.ntrp.cooldown_low_days', '30', 'int', 'global', 'NTRP 冷却期：可信度 <30（裁定 D9 低档）', 1, 0),
('cfg0000000000000047', 'score.ntrp.cooldown_mid_days', '60', 'int', 'global', 'NTRP 冷却期：可信度 30–60（裁定 D9 中档）', 1, 0),
('cfg0000000000000048', 'score.ntrp.cooldown_high_days', '90', 'int', 'global', 'NTRP 冷却期：可信度 ≥60（裁定 D9 高档）', 1, 0),

-- 5.8 评分域 · ELO（score.elo）4 项
('cfg0000000000000049', 'score.elo.initial', '1500', 'int', 'global', 'ELO 初始分', 1, 0),
('cfg0000000000000050', 'score.elo.k_factor', '32', 'int', 'global', 'K 系数', 1, 0),
('cfg0000000000000051', 'score.elo.blowout_offset', '0.1', 'float', 'global', '6-0/6-1 悬殊系数偏移（胜+0.1/负-0.1）', 1, 0),
('cfg0000000000000052', 'score.elo.close_offset', '0.05', 'float', 'global', '7-6/7-5 接近系数偏移（胜-0.05/负+0.05）', 1, 0),

-- 5.9 评价域（review）3 项
('cfg0000000000000053', 'review.deadline_days', '30', 'int', 'global', '评价/比分填写截止（finished 后 N 天）', 1, 0),
('cfg0000000000000054', 'review.tag.default_pool', '["正手稳","反手稳","发球好","底线稳","网前好","移动快","友善"]', 'json', 'global', '系统默认个性化标签池', 1, 0),
('cfg0000000000000055', 'review.tag.random_pick_count', '3', 'int', 'global', '从被评价人已有标签随机挑选展示数', 1, 0),

-- 5.10 通知域（notify）2 项
('cfg0000000000000056', 'notify.before_match_hours', '2', 'int', 'global', '赛前提醒提前量（小时）', 1, 0),
('cfg0000000000000057', 'notify.review_collect_hours', '1', 'int', 'global', '赛后评价催收延时（小时）', 1, 0),

-- 5.11 反滥用域（anti_abuse）5 项
('cfg0000000000000058', 'anti_abuse.publish_per_day_limit', '5', 'int', 'global', '单日发布上限（status=open/full）', 1, 0),
('cfg0000000000000059', 'anti_abuse.lower_vote_max_per_target', '3', 'int', 'global', '同人对同目标「低了」票计入校准度上限', 1, 0),
('cfg0000000000000060', 'anti_abuse.low_reputation_threshold', '30', 'int', 'global', '低信誉禁报名阈值（信誉分 < 此值）', 1, 0),
('cfg0000000000000061', 'anti_abuse.low_reputation_ban_days', '7', 'int', 'global', '低信誉禁报名天数', 1, 0),
('cfg0000000000000062', 'anti_abuse.conflict_buffer_minutes', '30', 'int', 'global', '报名抵冲路上缓冲时间（分钟）', 1, 0),

-- 5.12 约球域 · 城市开通（meetup.city）1 项
('cfg0000000000000063', 'meetup.city.opened_codes', '310000,110000,440100', 'json', 'global', '后台开通城市 cityCode 列表，发布/报名/推荐限定其中', 1, 0);

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
  `start_time`      DATETIME     NOT NULL COMMENT '活动开始时间（含日期，+08:00）',
  `end_time`        DATETIME     NOT NULL COMMENT '结束时间 = start_time + duration 小时，懒判定/兜底查询用（裁定 D3/D4）',
  `duration`        DECIMAL(3,1) NOT NULL COMMENT '持续小时：0.5/1.0/1.5/2.0/2.5/3.0',
  `court_name`      VARCHAR(128) DEFAULT NULL COMMENT '场地名称，手填',
  `court_address`   VARCHAR(256) NOT NULL COMMENT '场地详细地址，手填或地图点选',
  `court_lng`       DOUBLE       NOT NULL COMMENT '场地经度',
  `court_lat`       DOUBLE       NOT NULL COMMENT '场地纬度',
  `court_grid`      VARCHAR(64)  DEFAULT NULL COMMENT '场地冷启动去重键：场地名+50m网格（见 §3.4）',
  `level_mode`      varchar(8) DEFAULT 'exact' COMMENT '水平要求模式',
  `level_value`     VARCHAR(32)  DEFAULT NULL COMMENT '水平值，多值用冒号分割，如 range 存 "3.0:4.0"，exact 存 "3.5"',
  `gender_limit`    varchar(8) NOT NULL DEFAULT 'any' COMMENT '性别限制',
  `join_mode`       varchar(8) NOT NULL DEFAULT 'direct' COMMENT '加入模式：直接/审批',
  `cost_items`      JSON         DEFAULT NULL COMMENT '费用明细 [{name,totalAmount(分)}]，纯展示',
  `status`          varchar(8) NOT NULL DEFAULT 'open' COMMENT '状态机',
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

DROP TABLE IF EXISTS `rally_meetup_waitlist`;
CREATE TABLE `rally_meetup_waitlist` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务主键（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `user_id`         VARCHAR(32) NOT NULL COMMENT '报名人，关联 users.user_id',
  `status`          varchar(8) NOT NULL DEFAULT 'pending' COMMENT '报名状态机',
  `expires_at`      DATETIME    DEFAULT NULL COMMENT '自动撤回失效时间，NULL=不自动撤回',
  `opt_time`        DATETIME    DEFAULT NULL COMMENT '管理人审批操作时间',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_meetup_user_active` (`rally_meetup_id`, `user_id`) COMMENT '同人同场仅一条有效报名（撤回/拒绝后复报名见 §6.2 说明）',
  KEY `idx_user_status` (`user_id`, `status`) COMMENT '查我的报名 + 冲突检测',
  KEY `idx_meetup_status` (`rally_meetup_id`, `status`) COMMENT '审批列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约球报名/审核等待表';

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
  `review_value`    VARCHAR(64) NOT NULL COMMENT '评价值。ntrp_vote: higher/same/lower；attendance: on_time/late/no_show；tag: 标签名（每标签一行）',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  -- 裁定 D12：含 review_value。ntrp_vote/attendance 因 value 受限实质每人各一行；tag 允许多行
  UNIQUE KEY `uk_review_dim` (`rally_meetup_id`,`from_user_id`,`to_user_id`,`review_type`,`review_value`),
  KEY `idx_to_user` (`to_user_id`,`review_type`) COMMENT '球员主页按被评价人+维度聚合',
  KEY `idx_meetup_from` (`rally_meetup_id`,`from_user_id`) COMMENT '查我在某场已评状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价竖表（NTRP三元投票/出勤/个性化标签，每维度值一行）';

-- ============================================================
-- 8. 评价域：比分记录表（按盘）
-- ============================================================

DROP TABLE IF EXISTS `rally_meetup_score`;
CREATE TABLE `rally_meetup_score` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `set_number`      INT         NOT NULL COMMENT '第几盘，从 1 开始',
  `set_format`      VARCHAR(16) NOT NULL COMMENT '赛制：games_4 / games_6 / tiebreak',
  `side_a_player1`  VARCHAR(32) NOT NULL COMMENT 'A 侧选手1 user_id（裁定 D13，单个）',
  `side_a_player2`  VARCHAR(32) DEFAULT NULL COMMENT 'A 侧选手2 user_id，单打为 NULL',
  `side_b_player1`  VARCHAR(32) NOT NULL COMMENT 'B 侧选手1 user_id',
  `side_b_player2`  VARCHAR(32) DEFAULT NULL COMMENT 'B 侧选手2 user_id，单打为 NULL',
  `side_a_score`    INT         NOT NULL COMMENT 'A 侧本盘比分（局数/抢七分）',
  `side_b_score`    INT         NOT NULL COMMENT 'B 侧本盘比分',
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
-- 10. 评分域：ELO 聚合表
-- ============================================================

DROP TABLE IF EXISTS `player_elo`;
CREATE TABLE `player_elo` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`      VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `user_id`     VARCHAR(32) NOT NULL COMMENT '关联 users.user_id',
  `elo_score`   FLOAT       NOT NULL DEFAULT 1500 COMMENT 'ELO 分，初始 1500（配置 score.elo.initial）',
  `match_count` INT         NOT NULL DEFAULT 0 COMMENT '已计入 ELO 的对局盘数，便于后续 K 衰减（MVP 仅记录）',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家 ELO 聚合表（后台撮合，不展示）';

-- ============================================================
-- 11. 评分域：批量评分幂等状态表
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
