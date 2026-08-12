-- Read-only checks to run before migration.
SELECT m.id, m.tournament_id, m.year, m.status, m.player1_id, m.player2_id, m.winner_id
FROM tour_match AS m
WHERE m.winner_id IS NOT NULL
  AND m.winner_id NOT IN (m.player1_id, m.player2_id);

SELECT m.id, m.tournament_id, m.year, m.status, COUNT(s.id) AS legacy_set_count
FROM tour_match AS m
JOIN tour_set_score AS s ON s.tour_match_id = m.id
WHERE UPPER(COALESCE(m.status, '')) NOT IN ('LIVE', 'FINISHED')
   OR m.id IN (2775, 2776)
GROUP BY m.id, m.tournament_id, m.year, m.status;

-- Read-only checks to run after migration.
SELECT
    SUM(sets_json IS NOT NULL) AS matches_with_json,
    SUM(sets_json IS NULL) AS matches_without_json,
    SUM(sets_json IS NOT NULL AND JSON_VALID(sets_json) = 0) AS invalid_json
FROM tour_match;

SELECT m.id, COUNT(s.id) AS legacy_set_count, JSON_LENGTH(m.sets_json) AS json_set_count
FROM tour_match AS m
JOIN tour_set_score AS s ON s.tour_match_id = m.id
WHERE m.sets_json IS NOT NULL
GROUP BY m.id, m.sets_json
HAVING legacy_set_count <> json_set_count;

SELECT id, tournament_id, year, player1_id, player2_id, winner_id, sets_json
FROM tour_match
WHERE id IN (2775, 2776);
