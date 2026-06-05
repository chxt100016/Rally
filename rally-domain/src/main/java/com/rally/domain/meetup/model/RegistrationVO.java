package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批列表条目视图（报名人摘要 + 状态）
 */
@Data
public class RegistrationVO {
    private String registrationId;
    private String meetupId;
    private String userId;
    private RegistrationStatusEnum status;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;

    // 报名人信息
    private String nickname;
    private String avatarUrl;
    private BigDecimal ntrpScore;
    private Integer reviewCount;
}
