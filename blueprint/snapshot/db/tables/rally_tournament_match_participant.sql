CREATE TABLE `rally_tournament_match_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_id` VARCHAR(32) NOT NULL COMMENT '雪花ID',
    `match_id` VARCHAR(32) NOT NULL COMMENT '关联比赛bizId',
    `tournament_id` VARCHAR(32) NOT NULL COMMENT '赛事bizId（冗余，便于查询）',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `entry_no` INT NOT NULL COMMENT '报名编号（冗余自rally_tournament_entry，双打同队两条记录相同）',
    `confirm_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '赛约确认状态：PENDING/CONFIRMED/REJECTED',
    `confirm_time` DATETIME DEFAULT NULL COMMENT '赛约确认时间',
    `result_confirm_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '结果确认状态：PENDING/CONFIRMED/REJECTED',
    `result_confirm_time` DATETIME DEFAULT NULL COMMENT '结果确认时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    UNIQUE KEY `uk_match_user` (`match_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事比赛参与者表';
