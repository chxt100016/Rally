package com.rally.domain.meetup.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建人信息 DTO
 */
@Data
public class CreatorDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal ntrpScore;
    /** 发布比赛次数 */
    private Long publishMeetupCount;
}
