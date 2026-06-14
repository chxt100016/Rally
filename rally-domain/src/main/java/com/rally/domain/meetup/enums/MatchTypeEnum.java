package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 约球类型枚举
 */
@AllArgsConstructor
@Getter
public enum MatchTypeEnum {
    SINGLE("单打"),
    DOUBLE("双打"),
    RALLY("拉球")
    ;

    public final String name;
}
