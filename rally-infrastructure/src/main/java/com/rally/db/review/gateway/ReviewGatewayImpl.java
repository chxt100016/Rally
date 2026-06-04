package com.rally.db.review.gateway;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.repository.ReviewRepository;
import com.rally.domain.review.gateway.ReviewGateway;
import com.rally.domain.review.model.ReviewData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 评价竖表网关实现
 */
@Component
@RequiredArgsConstructor
public class ReviewGatewayImpl implements ReviewGateway {

    private final ReviewRepository reviewRepository;
    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    @Override
    public void upsert(String rallyMeetupId, String fromUserId, String toUserId,
                       String reviewType, List<String> reviewValues) {
        // 先删后插：保证 ntrp_vote/attendance 各一行，tag 可多行
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
        reviewRepository.deleteAndInsert(rallyMeetupId, fromUserId, toUserId, reviewType, newRecords);
    }

    @Override
    public List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId,
                                            String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(
                reviewRepository.findByDimension(rallyMeetupId, fromUserId, toUserId, reviewType));
    }

    @Override
    public List<ReviewData> listByMeetupAndFrom(String rallyMeetupId, String fromUserId) {
        return MAPPER.toReviewDataList(
                reviewRepository.listByMeetupAndFrom(rallyMeetupId, fromUserId));
    }

    @Override
    public List<ReviewData> listByToUserAndType(String toUserId, String reviewType) {
        return MAPPER.toReviewDataList(
                reviewRepository.listByToUserAndType(toUserId, reviewType));
    }

    @Override
    public List<Object[]> countByToUserGroupByValue(String toUserId, String reviewType) {
        // 从 repository 获取数据后在内存中聚合
        List<ReviewPO> list = reviewRepository.listByToUserAndTypeForAggregation(toUserId, reviewType);
        java.util.Map<String, Long> countMap = new java.util.LinkedHashMap<>();
        for (ReviewPO po : list) {
            countMap.merge(po.getReviewValue(), 1L, Long::sum);
        }
        List<Object[]> result = new ArrayList<>();
        countMap.forEach((value, count) -> result.add(new Object[]{value, count}));
        return result;
    }

    @Override
    public List<String> listDistinctValuesByToUserAndType(String toUserId, String reviewType) {
        return reviewRepository.listDistinctValuesByToUserAndType(toUserId, reviewType);
    }
}
