package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名/注册领域数据对象（记录所有参与者：创建者、等待审批、已通过等）
 */
@Data
public class RegistrationData {
    private String bizId;
    private String rallyMeetupId;
    private String userId;
    private WaitlistStatusEnum status;
    private LocalDateTime expiresAt;
    private LocalDateTime optTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
