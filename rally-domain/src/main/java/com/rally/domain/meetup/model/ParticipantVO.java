package com.rally.domain.meetup.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 参与者视图
 */
@Data
public class ParticipantVO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal ntrpScore;
}
