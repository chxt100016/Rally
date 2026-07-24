package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 约球状态枚举
 */
@AllArgsConstructor
@Getter
public enum MeetupStatusEnum {
    DRAFT("草稿"),
    OPEN("报名中"),
    ONGOING("进行中"),
    CLOSED("关闭"),
    FINISHED("已结束");

    public final String label;

}
