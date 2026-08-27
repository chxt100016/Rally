package com.rally.recap;

import com.rally.domain.recap.model.MyReviewDTO;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.SkipReviewCmd;
import com.rally.meetup.activity.SkipMeetupReviewActivity;
import com.rally.meetup.activity.UpsertPeerReviewItemsActivity;
import com.rally.personalprofile.myreviewsummary.activity.AggregateMyReviewSummaryActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewAppService {

    private final SkipMeetupReviewActivity skipMeetupReviewActivity;
    private final UpsertPeerReviewItemsActivity upsertPeerReviewItemsActivity;
    private final AggregateMyReviewSummaryActivity aggregateMyReviewSummaryActivity;

    public void skipReview(SkipReviewCmd cmd) {
        String userId = UserContext.get();
        skipMeetupReviewActivity.execute(userId, cmd.getMeetupId());
    }

    public MyReviewDTO queryMyReview() {
        String userId = UserContext.get();
        return aggregateMyReviewSummaryActivity.execute(userId);
    }

    public void submitReview(ReviewSubmitCmd cmd) {
        String userId = UserContext.get();
        upsertPeerReviewItemsActivity.execute(userId, cmd);
    }
}
