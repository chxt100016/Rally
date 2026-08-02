-- 为已有赛事增加规则海报对象存储路径。
ALTER TABLE `rally_tournament`
    ADD COLUMN `rule_poster_key` VARCHAR(256) DEFAULT NULL COMMENT '规则海报图片key（对象存储）' AFTER `poster_key`;
