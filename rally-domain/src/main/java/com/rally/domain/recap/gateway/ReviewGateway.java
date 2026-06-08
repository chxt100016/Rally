package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ReviewData;

import java.util.List;

/**
 * 评价竖表读写网关接口
 */
public interface ReviewGateway {

    /**
     * 保存评价（先删后插，保证 ntrp_vote/attendance 各一行，tag 可多行）
     */
    void upsert(String rallyMeetupId, String fromUserId, String toUserId,
                String reviewType, List<String> reviewValues);

    /**
     * 查询某场某人对某人的指定维度评价
     */
    List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId,
                                     String toUserId, String reviewType);

    /**
     * 查询某场某人提交的所有评价
     */
    List<ReviewData> listByMeetupAndFrom(String rallyMeetupId, String fromUserId);

    /**
     * 查询某人收到的指定维度评价（球员主页聚合用）
     */
    List<ReviewData> listByToUserAndType(String toUserId, String reviewType);

    /**
     * 统计某人收到的指定维度评价按 value 分组计数
     * @return List of [review_value, count]
     */
    List<Object[]> countByToUserGroupByValue(String toUserId, String reviewType);

    /**
     * 查询某人收到的指定维度指定值的评价（标签推荐用）
     */
    List<String> listDistinctValuesByToUserAndType(String toUserId, String reviewType);

    /**
     * 查询某人收到的全部评价（一次查库，聚合计算总数+标签用）
     */
    List<ReviewData> listAllByToUser(String toUserId);
}
