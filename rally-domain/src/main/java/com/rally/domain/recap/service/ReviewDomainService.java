package com.rally.domain.recap.service;

import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.gateway.ReviewRepository;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewDomainService {

    private final ReviewRepository recapRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public void skipReview(Meetup meetup, String userId) {
        registrationRepository.toSkipped(userId, meetup.getMeetupId());
    }

    @Transactional
    public void submitReviewItems(Meetup meetup, String userId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {
        recapRepository.submitReviewItems(meetup.getMeetupId(), userId, toUserId, targetReviews);
        if (meetup.hasReview(userId)) {
            return;
        }
        List<String> waitlistIds = meetup.getReviewWaitlistIds(userId);
        if (waitlistIds.isEmpty()) {
            registrationRepository.toReviewed(userId, meetup.getMeetupId());
            return;
        }
        Set<String> reviewedIds = recapRepository.listReviewsByMeetupAndFrom(meetup.getMeetupId(), userId)
                .stream().map(ReviewData::getToUserId).collect(Collectors.toSet());
        if (reviewedIds.containsAll(waitlistIds)) {
            registrationRepository.toReviewed(userId, meetup.getMeetupId());
        }
    }

    public List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId) {
        return recapRepository.listReviewsByMeetupAndFrom(meetupId, fromUserId);
    }
}
