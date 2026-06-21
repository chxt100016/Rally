package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

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

    /** 创建人本次重新订阅授权成功的通知场景（用于补充 PENDING_APPROVAL 额度） */
    private List<String> acceptedNoticeScenes;
}
