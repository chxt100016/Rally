package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 球员主页 - 评分信息
 */
@Data
@Accessors(chain = true)
public class PlayerHomeScoreDTO {

    /** 评分等级 (S/A/B/C) */
    private String profileLevel;
}
