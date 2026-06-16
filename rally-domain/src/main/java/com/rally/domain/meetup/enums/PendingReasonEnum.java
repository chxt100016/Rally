package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 待处理原因枚举（PENDING tab 用）
 */
@AllArgsConstructor
@Getter
public enum PendingReasonEnum {
    PENDING_APPROVAL("待审批"),
    UNREAD_MESSAGES("未读"),
    PENDING_REVIEW("待评价");

    public final String label;
}
