package com.rally.domain.recap.service;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.recap.gateway.RecapGateway;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreConflictException;
import com.rally.domain.recap.model.ScoreSubmitCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.score.ScoreManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 赛后收集领域服务
 * <p>
 * 职责：
 * 1. 加载聚合根（含业务校验：活动状态、截止时间、参与者）
 * 2. 提交评价（调用 gateway）
 * 3. 提交比分（调用 gateway，冲突抛 ScoreConflictException）
 * 4. 构建详情 VO
 * <p>
 * 注意：事务边界由应用层（RecapAppService）控制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecapDomainService {

    private final RecapGateway recapGateway;
    private final RegistrationGateway registrationGateway;
    private final ScoreManager scoreManager;



    /**
     * 提交评价（独立事务，调用 gateway 完成 diff 落库）
     * 提交后将 registration 状态从 JOINED → REVIEWED，用于 PENDING tab 判断待办
     */
    @Transactional
    public void submitReviewItems(Meetup meetup, String userId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {

        recapGateway.submitReviewItems(meetup.getMeetupId(), userId, targetReviews);
        // 评价已提交，标记 registration 状态为 REVIEWED
        registrationGateway.toReviewed(userId);

    }

    /**
     * 提交比分（独立事务，调用 gateway 完成版本校验与 diff 落库）
     */
    public void submitScoreItems(Meetup meetup, String userId, List<ScoreSubmitCmd.ScoreItem> targetScores, Integer clientVersion, LocalDateTime meetupDate, String venueName) {
        recapGateway.submitScoreItems(meetup.getMeetupId(), userId, targetScores, clientVersion, meetupDate, venueName);
        // 触发评分重算 TODO
    }

    // ==================== 查询 ====================

    /**
     * 查询某场某人提交的所有评价
     */
    public List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId) {
        return recapGateway.listReviewsByMeetupAndFrom(meetupId, fromUserId);
    }

    /**
     * 查询该活动的所有比分记录
     */
    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        return recapGateway.listScoresByMeetup(meetupId);
    }
}
