package com.rally.domain.recap.gateway;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.recap.model.Recap;
import com.rally.domain.recap.model.RecapSubmitCmd;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ScoreRecordData;

import java.util.List;

/**
 * 赛后收集网关接口
 * <p>
 * 职责：
 * 1. 加载聚合根所需数据（活动、参与人、评价、比分）
 * 2. 封装评价 diff 判断与落库
 * 3. 封装比分乐观锁校验与落库
 */
public interface RecapGateway {

    // ==================== 数据加载 ====================

    /**
     * 查询当前用户在某场的全部评价
     */
    List<ReviewData> listMyReviews(String meetupId, String userId);

    /**
     * 查询某场全部参与者 userId
     */
    List<String> listParticipantUserIds(String meetupId);

    /**
     * 查询活动数据
     */
    MeetupData findMeetupById(String meetupId);

    /**
     * 查询某场全部比分
     */
    List<ScoreRecordData> listScores(String meetupId);

    // ==================== 评价提交 ====================

    /**
     * 提交评价（gateway 内部完成 diff 判断与落库）
     *
     * @param meetupId   活动 ID
     * @param fromUserId 评价人
     * @param myReviews  当前用户已有评价
     * @param targetReviews 前端提交的目标评价
     */
    void submitReviews(String meetupId, String fromUserId,
                       List<ReviewData> myReviews, List<RecapSubmitCmd.ReviewItem> targetReviews);

    // ==================== 比分提交 ====================

    /**
     * 提交比分（gateway 内部完成版本校验、diff 判断与落库）
     * <p>
     * 版本冲突时抛出 ScoreConflictException。
     *
     * @param meetupId     活动 ID
     * @param userId       操作人
     * @param currentScores 当前比分快照
     * @param targetScores  前端提交的目标比分
     * @param clientVersion 前端回传的版本号
     */
    void submitScores(String meetupId, String userId,
                      List<ScoreRecordData> currentScores,
                      List<RecapSubmitCmd.ScoreItem> targetScores,
                      Integer clientVersion);
}
