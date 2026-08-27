package com.rally.personalprofile.playerhome.activity;

import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.user.model.MyProfileReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-player-review-summary：汇总目标球员收到的全部评价。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerReviewSummaryActivity {

    private static final int TOP_TAG_LIMIT = 5;

    private final UserReviewDomainService userReviewDomainService;

    public MyProfileReviewDTO execute(String targetUserId) {
        // A1-A3 复用原有一次读取与内存聚合，保留标签拆分、计数、排序及空结果语义。
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(targetUserId, TOP_TAG_LIMIT);
        return new MyProfileReviewDTO()
                .setTotal(summary.total())
                .setTags(summary.topTags());
    }
}
