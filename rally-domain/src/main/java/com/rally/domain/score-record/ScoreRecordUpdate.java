package com.rally.domain.meetup.scorerecord;

/**
 * C2 的非空字段更新载荷。
 *
 * <p>expectedVersion 仅是已完成的内存比较快照；newState 是保留 null 的稀疏更新实体。
 * SQL 只按 meetupId+scoreRecordId 定位，不带版本条件、不递增版本、不校验影响行数。</p>
 */
public record ScoreRecordUpdate(
        String meetupId,
        String scoreRecordId,
        int expectedVersion,
        ScoreRecordState newState) {
}
