package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 约球报名入参
 */
@Data
public class MeetupJoinCmd {

    /** 约球ID */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 自动撤回时间 */
    private LocalDateTime autoWithdrawAt;

    private String shareUserId;
}
