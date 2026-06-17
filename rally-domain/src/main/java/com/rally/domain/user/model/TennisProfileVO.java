package com.rally.domain.user.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 「我的档案」对外视图
 */
@Data
public class TennisProfileVO {

    private BigDecimal ntrpScore;
    private BigDecimal utrScore;
    private Boolean isNewbie;
    private Integer reputationScore;
    private Integer credibilityScore;
    private Integer calibrationScore;
    private String status;
    private Boolean isUnderReview;
    private Integer reviewRemainingMatches;
    private List<String> videos;
    private LocalDateTime ntrpUpdatedAt;
    private Boolean ntrpEditable;
    private Integer ntrpCooldownRemainingDays;
}
