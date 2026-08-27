package com.rally.domain.meetup.peerreview;

import java.util.Locale;

/** 评价维度及其 {@code rally_review.review_type} 持久化编码。 */
public enum PeerReviewType {
    LEVEL_VOTE("ntrp_vote"),
    ATTENDANCE_VOTE("attendance"),
    TAG("tag");

    private final String storageCode;

    PeerReviewType(String storageCode) {
        this.storageCode = storageCode;
    }

    public String storageCode() {
        return storageCode;
    }

    public static PeerReviewType fromStorageCode(String storageCode) {
        if (storageCode != null) {
            String normalized = storageCode.strip().toLowerCase(Locale.ROOT);
            for (PeerReviewType type : values()) {
                if (type.storageCode.equals(normalized)) {
                    return type;
                }
            }
        }
        throw new PeerReviewDomainException(
                PeerReviewSet.PEER_REVIEW_VALUE_INVALID,
                "未知的评价维度持久化编码");
    }
}
