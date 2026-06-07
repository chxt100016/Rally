package com.rally.domain.meetup.model;

/**
 * 退出报名结果
 */
public enum QuitResult {

    /** 正常退出，不扣分 */
    NORMAL,

    /** 临近开始（6h内）退出，需扣分 */
    PENALIZED
}
