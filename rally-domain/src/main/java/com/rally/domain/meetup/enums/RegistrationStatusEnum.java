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

    /** 参与态 */
    public static List<String> getParticipated() {
        return List.of(JOINED.name(), REVIEWED.name(), SKIPPED.name());
    }

    /** 完成态 */
    public static List<String> getCompleted() {
        return List.of(REVIEWED.name(), SKIPPED.name());
    }
}
