package com.rally.domain.profilechangelog.model;

/** I4 使用的来源事件幂等键。 */
public record ProfileChangeLogSourceKey(
        String userId,
        ProfileChangeLogType type,
        String refId,
        ProfileChangeReason reason) {
}
