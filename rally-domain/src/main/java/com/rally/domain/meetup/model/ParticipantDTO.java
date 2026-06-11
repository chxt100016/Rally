package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参与者视图
 */
@Data
public class ParticipantDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal ntrpScore;
    /** 性别 */
    private GenderEnum gender;
    /** 球友等级（S/A/B/C，空则前端不渲染） */
    private String profileLevel;
    /** 报名状态（创建人视角显示） */
    private RegistrationStatusEnum status;
    /** 报名记录 ID（创建人视角审批用） */
    private String registrationId;
    /** 申请时间 */
    private LocalDateTime applyTime;
}
