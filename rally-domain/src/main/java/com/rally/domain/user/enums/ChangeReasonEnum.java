package com.rally.domain.user.enums;

/**
 * 变更原因枚举
 */
public enum ChangeReasonEnum {
    /** 用户手动修改 */
    USER,
    /** 系统自动计算 */
    SYSTEM,
    /** 系统建议（校准度建议，跳过核查期） */
    SYSTEM_SUGGEST,
    /** 遇差票（核查期重置） */
    REVIEW_BAD
}
