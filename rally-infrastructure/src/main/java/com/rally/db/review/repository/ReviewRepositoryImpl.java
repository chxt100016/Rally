package com.rally.db.review.repository;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.service.ReviewService;
import com.rally.domain.recap.gateway.ReviewRepository;
import com.rally.domain.recap.model.ReviewData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewService reviewService;
    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

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
