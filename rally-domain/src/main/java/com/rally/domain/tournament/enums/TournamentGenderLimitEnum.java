package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛事性别限制枚举
 */
@AllArgsConstructor
@Getter
public enum TournamentGenderLimitEnum {
    ALL("不限"),
    MALE("限男生"),
    FEMALE("限女生");

    public final String label;
}
