-- 为已有赛事表增加 JSON 扩展数据，当前用于存储落地页主题颜色。
ALTER TABLE `rally_tournament`
    ADD COLUMN `ext_data` JSON DEFAULT NULL COMMENT '赛事扩展数据，当前存储主题颜色：buttonColor、backgroundColor' AFTER `match_rule_description`;
