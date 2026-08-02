package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝结果理由枚举
 */
@Getter
@AllArgsConstructor
public enum ResultRejectReasonEnum {

    DISPUTE_APPEAL("DISPUTE_APPEAL", "没发挥好，不服再战"),
    OPPONENT_LEVEL_MISMATCH("OPPONENT_LEVEL_MISMATCH", "对手水平明显超出赛事等级"),
    RESULT_INCORRECT("RESULT_INCORRECT", "比赛结果与实际不符"),
    ;

    private final String code;
    private final String label;

}
