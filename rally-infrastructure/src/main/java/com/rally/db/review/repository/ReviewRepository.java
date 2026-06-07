package com.rally.db.review.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评价竖表 Repository（门面层）
 */
@Repository
@RequiredArgsConstructor
public class ReviewRepository {

    private final ReviewService reviewService;

    /**
     * 先删后插：删除指定 (meetup, from, to, type) 的旧记录，再批量插入新值
     */
    public void deleteAndInsert(String rallyMeetupId, String fromUserId, String toUserId,
                                String reviewType, List<ReviewPO> newRecords) {
        // 删除旧记录
        reviewService.lambdaUpdate()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .remove();
        // 批量插入新记录
        if (newRecords != null && !newRecords.isEmpty()) {
            reviewService.saveBatch(newRecords);
        }
    }

    /**
     * 查询某场某人对某人的指定维度评价
     */
    public List<ReviewPO> findByDimension(String rallyMeetupId, String fromUserId,
                                          String toUserId, String reviewType) {
        return reviewService.lambdaQuery()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .list();
    }

    /**
     * 查询某场某人提交的所有评价
     */
    public List<ReviewPO> listByMeetupAndFrom(String rallyMeetupId, String fromUserId) {
        return reviewService.lambdaQuery()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .list();
    }

    /**
     * 查询某人收到的指定维度评价（球员主页聚合用）
     */
    public List<ReviewPO> listByToUserAndType(String toUserId, String reviewType) {
        return reviewService.lambdaQuery()
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .list();
    }

    /**
     * 查询某人收到的指定维度指定值的评价（标签推荐用）
     */
    public List<String> listDistinctValuesByToUserAndType(String toUserId, String reviewType) {
        return reviewService.lambdaQuery()
                .select(ReviewPO::getReviewValue)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .groupBy(ReviewPO::getReviewValue)
                .list()
                .stream()
                .map(ReviewPO::getReviewValue)
                .toList();
    }

    /**
     * 统计某人收到的指定维度按 value 分组计数
     * @return 每个元素 [reviewValue, count]
     */
    public List<ReviewPO> listByToUserAndTypeForAggregation(String toUserId, String reviewType) {
        return reviewService.lambdaQuery()
                .select(ReviewPO::getReviewValue)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .list();
    }

    /**
     * 查询某场某人对某人的全部评价（用于判断是否已评）
     */
    public List<ReviewPO> listByMeetupAndFromAndTo(String rallyMeetupId, String fromUserId, String toUserId) {
        return reviewService.lambdaQuery()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .eq(ReviewPO::getToUserId, toUserId)
                .list();
    }

    /**
     * 新增单条评价
     */
    public void save(ReviewPO po) {
        reviewService.save(po);
    }

    /**
     * 按维度更新评价值（meetupId + fromUser + toUser + type）
     */
    public void updateValue(String rallyMeetupId, String fromUserId,
                            String toUserId, String reviewType, String newValue) {
        reviewService.lambdaUpdate()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .set(ReviewPO::getReviewValue, newValue)
                .update();
    }

    /**
     * 按维度删除评价（meetupId + fromUser + toUser + type）
     */
    public void deleteByDimension(String rallyMeetupId, String fromUserId,
                                  String toUserId, String reviewType) {
        reviewService.lambdaUpdate()
                .eq(ReviewPO::getRallyMeetupId, rallyMeetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .eq(ReviewPO::getToUserId, toUserId)
                .eq(ReviewPO::getReviewType, reviewType)
                .remove();
    }
}
