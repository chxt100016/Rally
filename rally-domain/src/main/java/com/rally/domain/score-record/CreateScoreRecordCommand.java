package com.rally.domain.meetup.scorerecord;

/** C1 新增一盘比分。 */
public record CreateScoreRecordCommand(
        ScoreRecordingEligibility eligibility,
        String meetupId,
        ScoreRecordDraft draft) {
}
