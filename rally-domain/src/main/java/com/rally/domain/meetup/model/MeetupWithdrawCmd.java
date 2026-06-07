package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 撤回报名入参
 */
@Data
public class MeetupWithdrawCmd {

    /** 约球ID */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;
}
