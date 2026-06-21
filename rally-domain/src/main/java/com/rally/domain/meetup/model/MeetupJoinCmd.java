package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 本次微信订阅授权成功的通知场景（NoticeScene name 列表） */
    private List<String> acceptedNoticeScenes;
}
