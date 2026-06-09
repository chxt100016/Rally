package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报名结果
 */
@Getter
@AllArgsConstructor
public enum JoinResult {
    /** 直接加入 */
    APPROVED("直接加入"),
    /** 等待审批 */
    PENDING("等待审批");

    private final String desc;

    /**
     * 根据报名记录状态判断报名结果
     * @param registration 报名记录
     * @return 报名结果
     */
    public static JoinResult fromRegistration(RegistrationData registration) {
        return registration.getStatus() == RegistrationStatusEnum.JOINED ? APPROVED : PENDING;
    }
}
