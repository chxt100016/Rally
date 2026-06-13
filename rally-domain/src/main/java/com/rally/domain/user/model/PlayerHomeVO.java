package com.rally.domain.user.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 球员主页（公开页）视图
 */
@Data
public class PlayerHomeVO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String cityCode;
    private String bio;
    private BigDecimal ntrpScore;
    private Boolean isNewbie;
    private Boolean isUnderReview;
    private List<String> videos;
    private LocalDateTime joinTime;
    private Integer meetupCompletedCount;
    private Integer reviewReceivedCount;
    // 标签云（依赖评价域 03，MVP 预留）
    // private List<TagCloudItem> tagCloud;
    // 最近约球记录（依赖约球域 02，MVP 预留）
    // private List<MeetupBriefVO> recentMeetups;
}
