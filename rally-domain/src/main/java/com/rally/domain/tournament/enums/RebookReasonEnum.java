package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打回重订理由枚举
 */
@Getter
@AllArgsConstructor
public enum RebookReasonEnum {

    TIME_NOT_SUITABLE("TIME_NOT_SUITABLE", "时间不合适"),
    PLACE_NOT_SUITABLE("PLACE_NOT_SUITABLE", "地点不合适"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

}
