package com.rally.db.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_tennis_profile")
public class TennisProfilePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String userId;
    /** JSON 格式存储视频列表 [{key,title}] */
    private String videos;
    private BigDecimal ntrpScore;
    private BigDecimal utrScore;
    private LocalDateTime ntrpUpdatedAt;
    /** tbc/normal/under_review */
    private String status;
    private BigDecimal reputationScore;
    private BigDecimal credibilityScore;
    private BigDecimal calibrationScore;
    private Boolean isUnderReview;
    private Integer reviewRemainingMatches;
    private Boolean isNewbie;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
