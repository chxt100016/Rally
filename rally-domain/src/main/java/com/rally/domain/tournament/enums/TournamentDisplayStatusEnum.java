package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛事展示状态枚举（基于时间计算）
 */
@AllArgsConstructor
@Getter
public enum TournamentDisplayStatusEnum {
    NOT_STARTED("未开始"),
    REGISTRATION("报名中"),
    IN_PROGRESS("进行中"),
    ENDED("已结束"),
    ABANDONED("已废弃");

    public final String label;
}
