package com.rally.domain.recap.enums;

/**
 * 赛后评价类型枚举
 * 一场比赛对同一球员每种类型仅一条评价
 */
public enum RecapTypeEnum {
    /** 水平三元投票（高了/差不多/低了） */
    LEVEL,
    /** 出勤投票（准时/迟到/爽约） */
    PRESENT
}
