package com.rally.personalprofile.myreviewsummary.activity;

import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.recap.model.MyReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 aggregate-my-review-summary：汇总当前用户收到的全部评价。
 */
@Component
@RequiredArgsConstructor
public class AggregateMyReviewSummaryActivity {

    private static final int TOP_TAG_LIMIT = 5;

    private final UserReviewDomainService userReviewDomainService;

    public MyReviewDTO execute(String userId) {
        // A1-A4 复用原有的一次读取与内存聚合，保留标签拆分、计数、排序及空结果语义。
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, TOP_TAG_LIMIT);
        return new MyReviewDTO()
                .setTotal(summary.total())
                .setLevelVoteCount(summary.levelVoteCount())
                .setAttendanceVoteCount(summary.attendanceVoteCount())
                .setTagCount(summary.tagCount())
                .setTags(summary.topTags());
    }
}
