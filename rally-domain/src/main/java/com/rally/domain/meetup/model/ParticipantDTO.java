package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 参与者视图
 */
@Data
public class ParticipantDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal ntrpScore;
    /** 报名状态（创建人视角显示） */
    private RegistrationStatusEnum status;
}
