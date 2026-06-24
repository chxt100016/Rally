package com.rally.domain.meetup.enums;

import java.util.List;

/**
 * 报名状态枚举
 */
public enum RegistrationStatusEnum {
    PENDING,
    JOINED,
    REJECTED,
    WITHDRAWN,
    QUIT,
    REVIEWED,
    SKIPPED,

    ;

    /** 视为"已参与活动"的报名状态（用于用户维度查询） */
    public static List<String> getParticipated() {
        return List.of(JOINED.name(), REVIEWED.name(), SKIPPED.name());
    }
}
