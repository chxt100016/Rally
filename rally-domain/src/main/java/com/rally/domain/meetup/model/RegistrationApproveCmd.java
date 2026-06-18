package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批通过入参
 */
@Data
public class RegistrationApproveCmd {

    /** 报名ID */
    @NotBlank(message = "活动id不能为空")
    private String meetupId;

    /** 报名ID */
    @NotBlank(message = "报名ID不能为空")
    private String registrationId;
}
