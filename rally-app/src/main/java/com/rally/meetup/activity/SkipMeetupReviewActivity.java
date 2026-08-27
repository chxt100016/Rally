package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.service.ReviewDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 skip-meetup-review：宽松地跳过当前用户的约球评价。
 */
@Component
@RequiredArgsConstructor
public class SkipMeetupReviewActivity {

    private final MeetupDomainService meetupDomainService;

    private final ReviewDomainService reviewDomainService;

    public void execute(String userId, String meetupId) {
        // A1-A2：只核实约球实际状态，不校验参与资格或评价截止期。
        Meetup meetup = meetupDomainService.get(meetupId);
        meetup.assertCanReview();

        // A3：仓储按 userId + meetupId + JOINED 批量转 SKIPPED；零行也成功。
        reviewDomainService.skipReview(meetup, userId);
    }
}
