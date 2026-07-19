package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邀请用户加入活动入参
 */
@Data
public class MeetupInviteCmd {

    /** 约球ID */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 被邀请人用户ID */
    @NotBlank(message = "被邀请人用户ID不能为空")
    private String userId;
}
