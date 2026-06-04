package com.rally.domain.score.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评分维度枚举（内部路由用，非落库）
 */
@Getter
@AllArgsConstructor
public enum ScoreDimensionEnum {

    /** 信誉分（行为驱动增量） */
    REPUTATION("reputation"),
    /** 可信度（全量重算） */
    CREDIBILITY("credibility"),
    /** 校准度（全量重算） */
    CALIBRATION("calibration"),
    /** ELO（仅有比分时触发） */
    ELO("elo");

    private final String value;
}
