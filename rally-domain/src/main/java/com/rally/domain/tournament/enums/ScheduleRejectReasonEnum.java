package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝比赛理由枚举
 */
@Getter
@AllArgsConstructor
public enum ScheduleRejectReasonEnum {

    TIME_PLACE_CONFLICT("TIME_PLACE_CONFLICT", "时间或场地无法协调"),
    DONT_WANT_PLAY("DONT_WANT_PLAY", "暂不想参加这场比赛"),
    ;

    private final String code;
    private final String label;

}
