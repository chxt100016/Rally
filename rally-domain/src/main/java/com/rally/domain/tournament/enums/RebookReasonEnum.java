package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打回重订理由枚举
 */
@Getter
@AllArgsConstructor
public enum RebookReasonEnum {

    TIME_NOT_SUITABLE("TIME_NOT_SUITABLE", "比赛时间不合适"),
    PLACE_NOT_SUITABLE("PLACE_NOT_SUITABLE", "比赛地点不合适"),
    DURATION_NOT_SUITABLE("DURATION_NOT_SUITABLE", "时长不合适");

    private final String code;
    private final String label;

}
