-- 比赛理由统一只存枚举 code，不再保存自由文本。
ALTER TABLE `rally_tournament_match`
    DROP COLUMN `reject_reason_text`,
    DROP COLUMN `last_rebook_reason_text`;


