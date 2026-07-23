-- ============================================================
-- 赛事域：rally_tournament（赛事表）
-- ============================================================

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
    `end_time` DATETIME DEFAULT NULL COMMENT '赛事结束时间，赛事结束后写入',
    `qualifier_reject_limit` INT NOT NULL DEFAULT 1 COMMENT '资格赛阶段拒绝次数上限',
    `main_draw_reject_limit` INT NOT NULL DEFAULT 1 COMMENT '正赛阶段拒绝次数上限',
    `match_rule_description` TEXT DEFAULT NULL COMMENT '比赛规则描述，纯文本，支持\n换行',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/ACTIVE/ABANDONED',
    `current_filled_slots` INT NOT NULL DEFAULT 0 COMMENT '当前已支付锁定的正赛席位数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    KEY `idx_city_status` (`city_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事表';

-- ============================================================
-- 赛事域：rally_tournament_entry（赛事报名表）
-- ============================================================

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
    `stage` VARCHAR(8) NOT NULL DEFAULT 'QUALIFY' COMMENT '阶段：QUALIFY(资格赛)/MAIN(正赛)',
    `status` VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT '状态：WAITING(排队匹配)/IN_MATCH(比赛中)/PAYING(待支付，仅QUALIFY阶段)/ELIMINATED(淘汰)/WITHDRAWN(主动退出)',
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

-- ============================================================
-- 赛事域：rally_tournament_match（赛事比赛表）
-- ============================================================

DROP TABLE IF EXISTS `rally_tournament_match`;
CREATE TABLE `rally_tournament_match` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `tournament_id` VARCHAR(32) NOT NULL COMMENT '赛事bizId',
    `match_no` INT NOT NULL COMMENT '赛事内展示编号，同一赛事内从1开始递增，前端展示补零为3位（如001/002），不跨赛事全局唯一',
    `round` VARCHAR(16) NOT NULL COMMENT '轮次：QUALIFIER/ROUND_32/...',
    `group_size` INT NOT NULL DEFAULT 2 COMMENT '本场人数（2或3）',
    `court_booker_id` VARCHAR(32) DEFAULT NULL COMMENT '订场人用户ID',
    `court_booker_selected_time` DATETIME DEFAULT NULL COMMENT '订场人确定时间',
    `court_name` VARCHAR(128) DEFAULT NULL COMMENT '球场名称',
    `court_address` VARCHAR(256) DEFAULT NULL COMMENT '球场地址',
    `court_lng` DOUBLE DEFAULT NULL COMMENT '场地经度',
    `court_lat` DOUBLE DEFAULT NULL COMMENT '场地纬度',
    `court_city_code` VARCHAR(16) DEFAULT NULL COMMENT '场地城市编码',
    `court_city_name` VARCHAR(32) DEFAULT NULL COMMENT '场地城市名称',
    `court_select_mode` VARCHAR(8) DEFAULT NULL COMMENT '球场选择模式：TEXT/MAP/FREE，与约球域保持一致',
    `court_id` VARCHAR(32) DEFAULT NULL COMMENT '球场库ID，TEXT/MAP模式下从球场库选中时传入',
    `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '约定的线下比赛开始时间',
    `scheduled_duration` DECIMAL(4,1) DEFAULT NULL COMMENT '约定时长，单位：小时，支持小数（如1.5）',
    `schedule_submitted_time` DATETIME DEFAULT NULL COMMENT '订场人提交赛约、进入SCHEDULED状态的时间，用于超时判定，每次退回BOOKING重新提交会更新',
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
    UNIQUE KEY `uk_tournament_match_no` (`tournament_id`, `match_no`),
    KEY `idx_tournament_round` (`tournament_id`, `round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事比赛表';

-- ============================================================
-- 赛事域：rally_tournament_match_participant（赛事比赛参与者表）
-- ============================================================

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
-- ALTER rally_meetup 添加 meetup_type 列
ALTER TABLE rally_meetup ADD COLUMN meetup_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '约球类型：NORMAL/TOURNAMENT' AFTER biz_id;
