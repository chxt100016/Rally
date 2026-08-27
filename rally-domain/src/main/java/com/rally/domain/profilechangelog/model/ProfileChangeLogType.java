package com.rally.domain.profilechangelog.model;

import java.util.Locale;

/** 当前聚合支持的档案变更类型。 */
public enum ProfileChangeLogType {
    NTRP,
    UNDER_REVIEW;

    /** 持久化统一使用小写 snake_case。 */
    public String persistenceValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProfileChangeLogType fromPersistenceValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
    }
}
