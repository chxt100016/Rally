package com.rally.domain.user.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 我的档案 - 等级信息
 */
@Data
public class MyProfileLevelDTO {

    /** NTRP 评分 */
    private BigDecimal ntrpScore;

    /** 自评修改剩余冷却天数 */
    private Integer lockday;

    /** 是否在核查期 */
    private Boolean isUnderReview;

    /** 核查期剩余比赛场次 */
    private Integer remainingMatches;

    /** 系统建议 */
    private LevelSuggestionDTO suggestion;
}
