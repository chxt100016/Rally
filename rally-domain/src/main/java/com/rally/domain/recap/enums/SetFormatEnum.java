package com.rally.domain.recap.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛制枚举
 */
@AllArgsConstructor
@Getter
public enum SetFormatEnum {
    /** 常规局 */
    GAME("局"),
    /** 抢七 */
    TIEBREAK("抢分")
    ;

    public final String show;
}
