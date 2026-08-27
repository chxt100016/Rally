package com.rally.domain.profilechangelog.model;

import java.util.Locale;

/** 当前建立命令使用的档案变更原因。 */
public enum ProfileChangeReason {
    USER;

    /** 持久化统一使用小写 snake_case。 */
    public String persistenceValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProfileChangeReason fromPersistenceValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
    }
}
