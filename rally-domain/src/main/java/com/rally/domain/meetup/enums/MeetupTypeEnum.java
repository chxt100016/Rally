package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 约球类型枚举
 */
@Getter
@AllArgsConstructor
public enum MeetupTypeEnum {

    NORMAL("NORMAL", "普通约球"),
    TOURNAMENT("TOURNAMENT", "赛事约球");

    private final String code;
    private final String label;

}
