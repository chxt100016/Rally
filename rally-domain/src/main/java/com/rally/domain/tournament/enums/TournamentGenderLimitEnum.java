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
    MALE("男子"),
    FEMALE("女子");

    public final String label;
}
