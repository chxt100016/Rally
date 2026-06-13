package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreSubmitCmd;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛后收集网关接口
 * <p>
 * 职责：
 * 1. 加载聚合根所需数据（评价、比分）
 * 2. 封装评价 diff 判断与落库
 * 3. 封装比分乐观锁校验与落库
 */
public interface RecapGateway {

    // ==================== 评价提交 ====================

    /**
     * 提交评价（gateway 内部完成 diff 判断与落库）
     *
     * @param meetupId      活动 ID
     * @param fromUserId    评价人
     * @param targetReviews 前端提交的目标评价
     */
    void submitReviewItems(String meetupId, String fromUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews);

    // ==================== 比分提交 ====================

    /**
     * 提交比分（gateway 内部完成版本校验、diff 判断与落库）
     * <p>
     * 版本冲突时抛出 ScoreConflictException。
     *
     * @param meetupId      活动 ID
     * @param userId        操作人
     * @param targetScores  前端提交的目标比分
     * @param clientVersion 前端回传的版本号
     * @param meetupDate    比赛日期
     * @param venueName     比赛场地名称
     */
    void submitScoreItems(String meetupId, String userId, List<ScoreSubmitCmd.ScoreItem> targetScores, Integer clientVersion, LocalDateTime meetupDate, String venueName);

    // ==================== 查询 ====================

    /**
     * 查询某场某人提交的所有评价
     */
    List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId);

    /**
     * 查询该活动的所有比分记录
     */
    List<ScoreRecordData> listScoresByMeetup(String meetupId);

    /**
     * 查询某用户参与的所有比分记录（按比赛日期倒序）
     */
    List<ScoreRecordData> listScoresByUserId(String userId);
}
