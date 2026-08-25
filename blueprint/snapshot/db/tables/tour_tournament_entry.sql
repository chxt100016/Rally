CREATE TABLE tour_tournament_entry (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    player_id     VARCHAR(50) NOT NULL COMMENT '外部球员ID',
    draw_id       BIGINT      COMMENT '签表ID，关联 tour_draw 表',
    seed          SMALLINT    COMMENT '种子号，NULL 表示非种子',
    entry_type    VARCHAR(10) NOT NULL DEFAULT 'DIRECT' COMMENT 'DIRECT / WILDCARD / QUALIFIER / LUCKY_LOSER',
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED / WITHDRAWN / RETIRED',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tour_entry_player_draw_year (draw_id, player_id),
    INDEX idx_tour_entry_player         (player_id),
    INDEX idx_tour_entry_draw           (draw_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员报名信息';
