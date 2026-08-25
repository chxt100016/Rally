CREATE TABLE tour_player (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    player_id     VARCHAR(50)  COMMENT '外部API返回的球员ID，如 S0AG, DH50',
    tour          VARCHAR(10)  NOT NULL DEFAULT 'ATP' COMMENT '所属巡回赛：ATP / WTA',
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    nationality   CHAR(3)      COMMENT 'ISO 3166-1 alpha-3，如 CHN / USA',
    birth_date    DATE,
    gender        CHAR(1)      COMMENT 'M / F',
    `rank`        INT          COMMENT '当前排名，NULL 表示未排名',
    points        INT          COMMENT '积分',
    hand          VARCHAR(10)  COMMENT 'RIGHT / LEFT / UNKNOWN',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- ATP 和 WTA 的 player_id 来自不同系统，可能重复，用 (player_id, tour) 作为唯一键
    UNIQUE KEY uk_tour_player_id_tour (player_id, tour),
    INDEX idx_tour_player_name    (last_name, first_name),
    INDEX idx_tour_player_rank    (`rank`),
    INDEX idx_tour_player_nation  (nationality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员基础信息';
