-- Phase 1: schema change. Execute explicitly during deployment; this file is not run by the application.
ALTER TABLE tour_match
    ADD COLUMN sets_json JSON NULL COMMENT '比赛各盘比分完整快照（player1/player2 方向）' AFTER match_date;

-- Phase 2: backfill only rows that pass the known safety rules.
-- IDs 2775/2776 are known contaminated records from event 806 and are deliberately excluded.
UPDATE tour_match AS m
SET m.sets_json = (
    SELECT CAST(CONCAT('[', GROUP_CONCAT(
        JSON_OBJECT(
            'setNumber', s.set_number,
            'p1Games', s.p1_games,
            'p2Games', s.p2_games,
            'p1Tiebreak', s.p1_tiebreak,
            'p2Tiebreak', s.p2_tiebreak
        ) ORDER BY s.set_number SEPARATOR ','
    ), ']') AS JSON)
    FROM tour_set_score AS s
    WHERE s.tour_match_id = m.id
)
WHERE m.sets_json IS NULL
  AND UPPER(m.status) IN ('LIVE', 'FINISHED')
  AND m.id NOT IN (2775, 2776)
  AND (m.winner_id IS NULL OR m.winner_id IN (m.player1_id, m.player2_id))
  AND EXISTS (SELECT 1 FROM tour_set_score AS existing_score WHERE existing_score.tour_match_id = m.id);

-- Do not DROP the legacy table in this migration. After an observation period, archive it explicitly:
-- RENAME TABLE tour_set_score TO tour_set_score_backup_202608;
