package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批拒绝入参
 */
@Data
public class RegistrationRejectCmd {

    /** 报名ID */
    @NotBlank(message = "报名ID不能为空")
    private String registrationId;
}
