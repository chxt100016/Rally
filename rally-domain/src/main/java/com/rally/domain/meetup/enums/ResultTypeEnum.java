package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 比赛结果枚举
 */
@AllArgsConstructor
@Getter
public enum ResultTypeEnum {
    WIN("胜"),
    LOSE("负")
    ;

    public final String show;
}
