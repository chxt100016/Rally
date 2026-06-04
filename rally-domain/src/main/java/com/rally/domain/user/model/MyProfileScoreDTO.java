package com.rally.domain.user.model;

import lombok.Data;

import java.util.List;

/**
 * 我的档案 - 评分信息
 */
@Data
public class MyProfileScoreDTO {

    /** 评分等级 (S/A/B/C) */
    private String scoreLevel;

    /** 评分明细列表 */
    private List<ScoreItemDTO> data;
}
