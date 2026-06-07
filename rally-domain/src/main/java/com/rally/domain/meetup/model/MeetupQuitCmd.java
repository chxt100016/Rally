package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退出约球入参
 */
@Data
public class MeetupQuitCmd {

    /** 约球ID */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;
}
