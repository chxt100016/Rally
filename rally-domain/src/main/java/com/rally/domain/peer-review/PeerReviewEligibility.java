package com.rally.domain.meetup.peerreview;

/** 由约球聚合在边界外计算并传入的可评价结论。 */
public enum PeerReviewEligibility {
    ALLOWED,
    REVIEWER_INELIGIBLE,
    DEADLINE_EXPIRED;

    public boolean isAllowed() {
        return this == ALLOWED;
    }
}
