package com.rally.domain.profilechangelog.model;

/** C2 记录核查触发的命令入参。 */
public record RecordReviewTriggerCommand(
        String userId,
        Integer requiredMatches,
        ProfileChangeReason reason,
        String remark,
        String refId) {
}
