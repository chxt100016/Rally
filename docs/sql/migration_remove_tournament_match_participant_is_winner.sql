-- 先将旧参与者表中的胜者标记回填至比赛主表，再删除冗余字段。
UPDATE `rally_tournament_match` AS match_record
JOIN (
    SELECT `match_id`, MIN(`entry_no`) AS `winner_entry_no`
    FROM `rally_tournament_match_participant`
    WHERE `is_winner` = 1
    GROUP BY `match_id`
) AS winner ON winner.`match_id` = match_record.`biz_id`
SET match_record.`winner_entry_no` = winner.`winner_entry_no`
WHERE match_record.`winner_entry_no` IS NULL;

ALTER TABLE `rally_tournament_match_participant`
    DROP COLUMN `is_winner`;
