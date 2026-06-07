package com.rally.domain.recap.enums;

/**
 * 整体提交状态
 */
public enum RecapOverallStatus {
    /** 全部成功 */
    ALL_SUCCESS,
    /** 评价成功、比分冲突 */
    PARTIAL,
    /** 全部失败 */
    ALL_FAIL
}
