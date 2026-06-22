package com.rally.db.review.gateway;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.service.ReviewService;
import com.rally.domain.recap.gateway.ReviewGateway;
import com.rally.domain.recap.model.ReviewData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviewGatewayImpl implements ReviewGateway {

    private final ReviewService reviewService;
    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    @Override
    public void upsert(String rallyMeetupId, String fromUserId, String toUserId, String reviewType, List<String> reviewValues) {
        List<ReviewPO> newRecords = new ArrayList<>();
        if (reviewValues != null) {
            for (String value : reviewValues) {
                ReviewPO po = new ReviewPO();
                po.setRallyMeetupId(rallyMeetupId);
                po.setFromUserId(fromUserId);
                po.setToUserId(toUserId);
                po.setReviewType(reviewType);
                po.setReviewValue(value);
                newRecords.add(po);
            }
        }
        reviewService.lambdaUpdate().eq(ReviewPO::getRallyMeetupId, rallyMeetupId).eq(ReviewPO::getFromUserId, fromUserId).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).remove();
        if (!newRecords.isEmpty()) {
            reviewService.saveBatch(newRecords);
        }
    }

    @Override
    public List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId, String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getRallyMeetupId, rallyMeetupId).eq(ReviewPO::getFromUserId, fromUserId).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).list());
    }

    @Override
    public List<ReviewData> listByMeetupAndFrom(String rallyMeetupId, String fromUserId) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getRallyMeetupId, rallyMeetupId).eq(ReviewPO::getFromUserId, fromUserId).list());
    }

    @Override
    public List<ReviewData> listByToUserAndType(String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).list());
    }

    @Override
    public List<Object[]> countByToUserGroupByValue(String toUserId, String reviewType) {
        List<ReviewPO> list = reviewService.lambdaQuery().select(ReviewPO::getReviewValue).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).list();
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (ReviewPO po : list) {
            countMap.merge(po.getReviewValue(), 1L, Long::sum);
        }
        List<Object[]> result = new ArrayList<>();
        countMap.forEach((value, count) -> result.add(new Object[]{value, count}));
        return result;
    }

    @Override
    public List<String> listDistinctValuesByToUserAndType(String toUserId, String reviewType) {
        return reviewService.lambdaQuery().select(ReviewPO::getReviewValue).eq(ReviewPO::getToUserId, toUserId).eq(ReviewPO::getReviewType, reviewType).groupBy(ReviewPO::getReviewValue).list().stream().map(ReviewPO::getReviewValue).toList();
    }

    @Override
    public List<ReviewData> listAllByToUser(String toUserId) {
        return MAPPER.toReviewDataList(reviewService.lambdaQuery().eq(ReviewPO::getToUserId, toUserId).list());
    }
}
