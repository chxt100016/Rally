package com.rally.domain.meetup.peerreview;

import java.util.Locale;

/** 出勤投票值及其持久化编码。 */
public enum AttendanceVote {
    ON_TIME("on_time"),
    LATE("late"),
    NO_SHOW("no_show");

    private final String storageValue;

    AttendanceVote(String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return storageValue;
    }

    static AttendanceVote parse(String value) {
        if (value != null) {
            String normalized = value.strip().toUpperCase(Locale.ROOT);
            for (AttendanceVote vote : values()) {
                if (vote.name().equals(normalized)
                        || vote.storageValue.equals(normalized.toLowerCase(Locale.ROOT))) {
                    return vote;
                }
            }
        }
        throw new PeerReviewDomainException(
                PeerReviewSet.PEER_REVIEW_VALUE_INVALID,
                "出勤评价只接受 ON_TIME、LATE 或 NO_SHOW");
    }
}
