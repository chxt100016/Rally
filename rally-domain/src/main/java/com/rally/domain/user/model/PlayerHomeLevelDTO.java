package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 球员主页 - 等级信息
 */
@Data
@Accessors(chain = true)
public class PlayerHomeLevelDTO {

    /** NTRP 评分 */
    private BigDecimal ntrpScore;

    /** 是否在核查期 */
    private Boolean isUnderReview;

    /** 是否新手 */
    private Boolean isNewbie;
}
