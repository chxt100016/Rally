package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝比赛理由枚举
 */
@Getter
@AllArgsConstructor
public enum ScheduleRejectReasonEnum {

    TIME_PLACE_CONFLICT("TIME_PLACE_CONFLICT", "时间/场地实在协调不了"),
    DONT_WANT_PLAY("DONT_WANT_PLAY", "不想打了"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

}
