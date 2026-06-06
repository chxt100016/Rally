package com.rally.domain.meetup.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报名结果
 */
@Getter
@AllArgsConstructor
public enum JoinResult {
    /** 直接加入 */
    APPROVED("直接加入"),
    /** 等待审批 */
    PENDING("等待审批");

    private final String desc;
}
