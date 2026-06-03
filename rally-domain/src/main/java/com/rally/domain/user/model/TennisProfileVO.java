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
    private String userId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String cityCode;
    private String bio;
    private BigDecimal ntrpScore;
    private BigDecimal utrScore;
    private String ratingLevel;
    private Boolean isNewbie;
    private BigDecimal reputationScore;
    private BigDecimal credibilityScore;
    private BigDecimal calibrationScore;
    private BigDecimal totalScore;
    private String status;
    private Boolean isUnderReview;
    private Integer reviewRemainingMatches;
    private List<String> videoUrls;
    private LocalDateTime ntrpUpdatedAt;
    private Boolean ntrpEditable;
    private Integer ntrpCooldownRemainingDays;
}
