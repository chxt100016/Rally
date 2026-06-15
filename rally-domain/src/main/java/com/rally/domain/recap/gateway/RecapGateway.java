package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;

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
     * 提交评价（一次一个被评价人，按维度 upsert：存在则更新，不存在则新增，不删旧）
     *
     * @param meetupId      活动 ID
     * @param fromUserId    评价人
     * @param toUserId      被评价人
     * @param targetReviews 前端提交的目标评价（每维度一条）
     */
    void submitReviewItems(String meetupId, String fromUserId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews);

    // ==================== 比分增删改 ====================

    /**
     * 新增比分（一次一盘，生成雪花 bizId；同场同盘重复抛 SCORE_SET_DUPLICATE）
     *
     * @param meetupId   活动 ID
     * @param userId     操作人
     * @param cmd        新增比分内容
     * @param meetupDate 比赛日期
     * @param venueName  比赛场地名称
     */
    void addScore(String meetupId, String userId, ScoreAddCmd cmd, LocalDateTime meetupDate, String venueName);

    /**
     * 修改比分（按 bizId 定位、version 乐观锁；版本不一致抛 SCORE_VERSION_MISMATCH，记录不存在抛 RECAP_SCORE_NOT_FOUND）
     *
     * @param meetupId   活动 ID
     * @param userId     操作人
     * @param cmd        修改比分内容（含 bizId 与 version）
     * @param meetupDate 比赛日期
     * @param venueName  比赛场地名称
     */
    void updateScore(String meetupId, String userId, ScoreUpdateCmd cmd, LocalDateTime meetupDate, String venueName);

    /**
     * 删除比分（按 bizId 定位，幂等；bizId 永不复用，天然防 ABA）
     *
     * @param meetupId 活动 ID
     * @param bizId    目标盘记录 bizId
     */
    void deleteScore(String meetupId, String bizId);

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
