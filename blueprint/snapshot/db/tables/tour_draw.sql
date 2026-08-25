CREATE TABLE tour_draw (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    tournament_id VARCHAR(50) NOT NULL COMMENT '外部赛事ID',
    year          INT         NOT NULL DEFAULT 2026 COMMENT '赛事年份',
    draw_type     VARCHAR(10) NOT NULL COMMENT 'MS / WS / MD / WD / XD',
    size          INT         NULL COMMENT '签表人数：32 / 64 / 128',
    total_rounds  INT         NULL COMMENT '总轮数，由 size 决定',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tour_draw_tournament_year_type (tournament_id, year, draw_type),
    INDEX idx_tour_draw_tournament (tournament_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签表（赛事下的具体项目）';
