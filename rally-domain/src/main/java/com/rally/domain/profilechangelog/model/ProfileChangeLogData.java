package com.rally.domain.profilechangelog.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 不可变的档案变更日志状态快照。 */
public record ProfileChangeLogData(
        Long id,
        String bizId,
        String userId,
        ProfileChangeLogType type,
        BigDecimal beforeValue,
        BigDecimal afterValue,
        BigDecimal value,
        ProfileChangeReason reason,
        String remark,
        String refId,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static ProfileChangeLogData newRecord(
            String bizId,
            String userId,
            ProfileChangeLogType type,
            BigDecimal beforeValue,
            BigDecimal afterValue,
            BigDecimal value,
            ProfileChangeReason reason,
            String remark,
            String refId) {
        return new ProfileChangeLogData(
                null,
                bizId,
                userId,
                type,
                beforeValue,
                afterValue,
                value,
                reason,
                remark,
                refId,
                null,
                null);
    }

    public ProfileChangeLogSourceKey sourceKey() {
        return new ProfileChangeLogSourceKey(userId, type, refId, reason);
    }
}
