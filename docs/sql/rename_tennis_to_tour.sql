-- 重命名 tennis 表为 tour 表
-- 执行顺序：先重命名表，再重命名索引

-- 1. 重命名表
RENAME TABLE tennis_player TO tour_player;
RENAME TABLE tennis_tournament TO tour_tournament;
RENAME TABLE tennis_draw TO tour_draw;
RENAME TABLE tennis_tournament_entry TO tour_tournament_entry;
RENAME TABLE tennis_match TO tour_match;
RENAME TABLE tennis_set_score TO tour_set_score;
RENAME TABLE user_tennis_profile TO user_tour_profile;

-- 2. 重命名索引（tour_draw 表）
ALTER TABLE tour_draw DROP INDEX uk_tennis_draw_tournament_year;
ALTER TABLE tour_draw ADD UNIQUE KEY uk_tour_draw_tournament_year (tournament_id, year, draw_type);

-- 3. 重命名索引（tour_tournament_entry 表）
ALTER TABLE tour_tournament_entry DROP INDEX uk_tennis_entry_player_draw_year;
ALTER TABLE tour_tournament_entry ADD UNIQUE KEY uk_tour_entry_player_draw_year (draw_id, player_id);
ALTER TABLE tour_tournament_entry DROP INDEX idx_tennis_entry_player;
ALTER TABLE tour_tournament_entry ADD INDEX idx_tour_entry_player (player_id);
ALTER TABLE tour_tournament_entry DROP INDEX idx_tennis_entry_draw;
ALTER TABLE tour_tournament_entry ADD INDEX idx_tour_entry_draw (draw_id);

-- 4. 重命名索引（tour_match 表）
ALTER TABLE tour_match DROP INDEX uk_tennis_match_draw_round_position;
ALTER TABLE tour_match ADD UNIQUE KEY uk_tour_match_draw_round_position (draw_id, round, match_position);
ALTER TABLE tour_match DROP INDEX idx_tennis_match_draw_id;
ALTER TABLE tour_match ADD INDEX idx_tour_match_draw_id (draw_id);
ALTER TABLE tour_match DROP INDEX idx_tennis_match_player1;
ALTER TABLE tour_match ADD INDEX idx_tour_match_player1 (player1_id);
ALTER TABLE tour_match DROP INDEX idx_tennis_match_player2;
ALTER TABLE tour_match ADD INDEX idx_tour_match_player2 (player2_id);

-- 5. 重命名索引（tour_set_score 表）
ALTER TABLE tour_set_score DROP INDEX uk_tennis_set_match_id_set_number;
ALTER TABLE tour_set_score ADD UNIQUE KEY uk_tour_set_match_id_set_number (tour_match_id, set_number);
ALTER TABLE tour_set_score DROP INDEX idx_tennis_set_tour_match_id;
ALTER TABLE tour_set_score ADD INDEX idx_tour_set_tour_match_id (tour_match_id);

-- 6. 重命名字段（tour_set_score 表）
ALTER TABLE tour_set_score CHANGE COLUMN tennis_match_id tour_match_id BIGINT NOT NULL COMMENT '关联 tour_match.id';

-- 7. 重命名索引（user_tour_profile 表）
ALTER TABLE user_tour_profile DROP INDEX uk_tennis_profile_user;
ALTER TABLE user_tour_profile ADD UNIQUE KEY uk_tour_profile_user (user_id);
ALTER TABLE user_tour_profile DROP INDEX idx_tennis_profile_user_id;
ALTER TABLE user_tour_profile ADD INDEX idx_tour_profile_user_id (user_id);
