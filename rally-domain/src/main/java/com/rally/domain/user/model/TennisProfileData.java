package com.rally.domain.user.model;

import com.rally.domain.user.enums.ProfileStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 球员档案领域数据
 */
@Data
public class TennisProfileData {
    private String bizId;
    private String userId;
    private List<String> videoUrls;
    private BigDecimal ntrpScore;
    private BigDecimal utrScore;
    private LocalDateTime ntrpUpdatedAt;
    private ProfileStatusEnum status;
    private BigDecimal reputationScore;
    private BigDecimal credibilityScore;
    private BigDecimal calibrationScore;
    private Boolean isUnderReview;
    private Boolean isNewbie;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
