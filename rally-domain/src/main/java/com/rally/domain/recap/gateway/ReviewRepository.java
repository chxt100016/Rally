package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ReviewData;

import java.util.List;

/**
 * 评价竖表读写网关接口
 */
public interface ReviewRepository {

    /**
     * 查询某场某人对某人的指定维度评价
     */
    List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId,
                                     String toUserId, String reviewType);

    /**
     * 查询某人收到的指定维度评价（球员主页聚合用）
     */
    List<ReviewData> listByToUserAndType(String toUserId, String reviewType);

    /**
     * 查询某人收到的全部评价（一次查库，聚合计算总数+标签用）
     */
    List<ReviewData> listAllByToUser(String toUserId);
}
