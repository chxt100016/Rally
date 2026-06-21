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
    REVIEWED,

    ;

    /** 可读取 IM 消息的报名状态（已加入或已参赛复盘） */
    public static List<String> getImAvailable() {
        return List.of(JOINED.name(), REVIEWED.name());
    }
}
