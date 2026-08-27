package com.rally.domain.meetup.scorerecord;

/** C3 直接按约球编号和比分编号删除，无需预读。 */
public record RemoveScoreRecordCommand(
        ScoreRecordingEligibility eligibility,
        String meetupId,
        String scoreRecordId) {
}
