package com.rally.db.review.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.service.ReviewService;
import com.rally.domain.recap.gateway.ReviewRepository;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewService reviewService;
    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    @Override
    public void submitReviewItems(String meetupId, String fromUserId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {
        for (ReviewSubmitCmd.ReviewItem item : targetReviews) {
            ReviewPO existing = reviewService.lambdaQuery().eq(ReviewPO::getRallyMeetupId, meetupId).eq(ReviewPO::getFromUserId, fromUserId).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, item.getType().name()).one();
            if (existing != null) {
                reviewService.lambdaUpdate().eq(ReviewPO::getId, existing.getId()).set(ReviewPO::getReviewValue, item.getValue()).update();
            } else {
                reviewService.save(MAPPER.toReviewPO(item, IdWorker.getIdStr(), meetupId, fromUserId, toUserId));
            }
        }
    }

    @Override
    public List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getRallyMeetupId, meetupId).eq(ReviewPO::getFromUserId, fromUserId).list());
    }

    @Override
    public List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId, String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getRallyMeetupId, rallyMeetupId).eq(ReviewPO::getFromUserId, fromUserId).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).list());
    }

    @Override
    public List<ReviewData> listByToUserAndType(String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).list());
    }

    @Override
    public List<ReviewData> listAllByToUser(String toUserId) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getToUserId, toUserId).list());
    }
}
