package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝结果理由枚举
 */
@Getter
@AllArgsConstructor
public enum ResultRejectReasonEnum {

    DISPUTE_APPEAL("DISPUTE_APPEAL", "不服，我要申诉重来"),
    OPPONENT_LEVEL_MISMATCH("OPPONENT_LEVEL_MISMATCH", "对手水平明显超出本赛事等级"),
    RESULT_INCORRECT("RESULT_INCORRECT", "提交的结果不属实"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

}
