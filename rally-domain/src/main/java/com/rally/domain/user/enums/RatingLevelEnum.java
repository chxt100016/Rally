package com.rally.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 球友评级枚举（S/A/B/C）
 */
@Getter
@AllArgsConstructor
public enum RatingLevelEnum {

    /** 优秀（总分 ≥90） */
    S("S", "优秀"),
    /** 良好（75–89） */
    A("A", "良好"),
    /** 一般（55–74） */
    B("B", "一般"),
    /** 较差（<55） */
    C("C", "较差");

    /** 等级代码 */
    private final String code;

    /** 中文标签 */
    private final String label;
}
