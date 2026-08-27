package com.rally.domain.meetup.scorerecord;

/** C2 用新内容修正一盘比分；期望版本只在读取后做内存比较。 */
public record CorrectScoreRecordCommand(
        ScoreRecordingEligibility eligibility,
        String meetupId,
        String scoreRecordId,
        int expectedVersion,
        ScoreRecordDraft draft) {
}
