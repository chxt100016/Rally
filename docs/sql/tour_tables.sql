-- ============================================================
-- 网球赛程数据库建表语句（MySQL 8.0+）
-- 统一使用外部 API 返回的字符串 ID 作为关联字段
-- ============================================================

DROP TABLE IF EXISTS tour_player;
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

DROP TABLE IF EXISTS tour_tournament;
CREATE TABLE tour_tournament (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tournament_id VARCHAR(50)  COMMENT '外部API返回的赛事ID，如 1536',
    year         INT           NOT NULL DEFAULT 2026 COMMENT '赛事年份',
    name         VARCHAR(100)  NOT NULL COMMENT '赛事名称，如 Wimbledon',
    tour VARCHAR(10) NOT NULL  DEFAULT 'ATP' COMMENT 'ATP / WTA / ITF/ Grand',
    category     VARCHAR(20)   DEFAULT ''  NOT NULL COMMENT '',
    surface      VARCHAR(10)   NOT NULL COMMENT '',
    city         VARCHAR(50)   NOT NULL,
    country      VARCHAR(32)   COMMENT '',
    prize_money  INT           COMMENT '总奖金（USD）',
    prize_money_text  VARCHAR(50)    COMMENT '总奖金文本',
    status       VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active / completed',
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,
    image_path      VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '主图',
    background_path      VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '背景图',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tour_tournament_tournament_year (tournament_id, year),
    INDEX idx_tour_tournament_date     (start_date, end_date),
    INDEX idx_tour_tournament_status   (status),
    INDEX idx_tour_tournament_category (category),
    INDEX idx_tour_tournament_year     (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事主信息';

DROP TABLE IF EXISTS tour_draw;
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

DROP TABLE IF EXISTS tour_tournament_entry;
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

DROP TABLE IF EXISTS tour_match;
CREATE TABLE tour_match (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    match_id         VARCHAR(50) NOT NULL COMMENT '外部API返回的比赛ID，如 MS008',
    match_index      INT         COMMENT '比赛序号，从 match_id 中提取的数字部分，如 MS008 → 8',
    draw_id          BIGINT      NOT NULL COMMENT '签表ID',
    tournament_id    VARCHAR(50) NOT NULL COMMENT '外部赛事ID，便于按赛事查询',
    year             INT         NOT NULL COMMENT '赛事年份',
    round_number     TINYINT     COMMENT '轮次序号：1=首轮，7=决赛（128签）',
    round_name       VARCHAR(32) COMMENT 'R128 / R64 / R32 / R16 / QF / SF / F',
    player1_id       VARCHAR(50) COMMENT '外部球员ID，未确定对阵时允许为 NULL',
    player2_id       VARCHAR(50) COMMENT '外部球员ID，同上',
    winner_id        VARCHAR(50) COMMENT '外部球员ID，比赛结束前为 NULL',
    scheduled_at     DATETIME    COMMENT '计划开赛时间',
    scheduled_at_text VARCHAR(50) COMMENT 'NotBefore文本，如 Starts At',
    started_at       DATETIME    COMMENT '实际开始时间',
    ended_at         DATETIME    COMMENT '实际结束时间',
    court            VARCHAR(50) COMMENT '场地名称，如 Centre Court',
    court_seq        TINYINT     COMMENT '该球场第几场比赛',
    status           VARCHAR(20) NOT NULL DEFAULT '' COMMENT '',
    duration_minutes SMALLINT    COMMENT '比赛时长（分钟）',
    description TEXT COMMENT '比赛描述',
    match_date DATE COMMENT '比赛日期，从MatchDate中提取',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tour_match_match_id (match_id, draw_id),
    INDEX idx_tour_match_draw        (draw_id, round_number),
    INDEX idx_tour_match_tournament  (tournament_id),
    INDEX idx_tour_match_tournament_year (tournament_id, year),
    INDEX idx_tour_match_player1     (player1_id),
    INDEX idx_tour_match_player2     (player2_id),
    INDEX idx_tour_match_status_time (status, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='具体比赛场次';

DROP TABLE IF EXISTS tour_set_score;
CREATE TABLE tour_set_score (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    tour_match_id  BIGINT   NOT NULL COMMENT '关联 tour_match.id（自增主键），全局唯一，无跨赛事冲突',
    set_number       TINYINT  NOT NULL COMMENT '第几盘：1 / 2 / 3 ...',
    p1_games         TINYINT  NOT NULL DEFAULT 0 COMMENT 'player1 局数',
    p2_games         TINYINT  NOT NULL DEFAULT 0 COMMENT 'player2 局数',
    p1_tiebreak      TINYINT  COMMENT '抢七分数，NULL 表示该盘无抢七',
    p2_tiebreak      TINYINT  COMMENT '抢七分数，NULL 表示该盘无抢七',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tour_set_match_id_set_number (tour_match_id, set_number),
    INDEX idx_tour_set_tour_match_id (tour_match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每盘比分详情';