package com.rally.domain.meetup.peerreview;

/** C1 批次中的一项目标与评价维度值。 */
public record PeerReviewSubmission(
        String toUserId,
        PeerReviewType reviewType,
        String reviewValue) {

    public static PeerReviewSubmission levelVote(String toUserId, LevelVote vote) {
        return new PeerReviewSubmission(
                toUserId,
                PeerReviewType.LEVEL_VOTE,
                vote == null ? null : vote.name());
    }

    public static PeerReviewSubmission attendanceVote(
            String toUserId, AttendanceVote vote) {
        return new PeerReviewSubmission(
                toUserId,
                PeerReviewType.ATTENDANCE_VOTE,
                vote == null ? null : vote.name());
    }

    public static PeerReviewSubmission tag(String toUserId, String tags) {
        return new PeerReviewSubmission(toUserId, PeerReviewType.TAG, tags);
    }
}
