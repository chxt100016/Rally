package com.rally.recap;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.recap.model.MyReviewDTO;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.SkipReviewCmd;
import com.rally.domain.recap.service.ReviewDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewAppService {

    private final ReviewDomainService reviewDomainService;
    private final MeetupDomainService meetupDomainService;
    private final UserReviewDomainService userReviewDomainService;

    public void skipReview(SkipReviewCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertCanReview();
        reviewDomainService.skipReview(meetup, userId);
    }

    public MyReviewDTO queryMyReview() {
        String userId = UserContext.get();
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, 5);
        return new MyReviewDTO()
                .setTotal(summary.total())
                .setLevelVoteCount(summary.levelVoteCount())
                .setAttendanceVoteCount(summary.attendanceVoteCount())
                .setTagCount(summary.tagCount())
                .setTags(summary.topTags());
    }

    public void submitReview(ReviewSubmitCmd cmd) {
        cmd.getReviews().forEach(item -> ReviewSubmitCmd.assertValidReviewValue(item.getType(), item.getValue()));
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        reviewDomainService.submitReviewItems(meetup, userId, cmd.getToUserId(), cmd.getReviews());
    }
}
