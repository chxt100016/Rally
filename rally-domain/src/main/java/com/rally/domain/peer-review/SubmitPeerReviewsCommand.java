package com.rally.domain.meetup.peerreview;

import java.util.List;
import java.util.Set;

/** C1：提交一批同场评价项。 */
public record SubmitPeerReviewsCommand(
        PeerReviewEligibility eligibility,
        Set<String> validParticipantUserIds,
        List<PeerReviewSubmission> submissions) {
}
