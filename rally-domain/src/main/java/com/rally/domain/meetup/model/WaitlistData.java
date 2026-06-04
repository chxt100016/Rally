package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名领域数据对象
 */
@Data
public class WaitlistData {
    private String bizId;
    private String rallyMeetupId;
    private String userId;
    private WaitlistStatusEnum status;
    private LocalDateTime expiresAt;
    private LocalDateTime optTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
