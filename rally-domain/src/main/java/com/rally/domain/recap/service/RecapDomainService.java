package com.rally.domain.recap.service;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.recap.gateway.RecapRepository;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.score.ScoreManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final RecapRepository recapRepository;
    private final RegistrationRepository registrationRepository;
    private final ScoreManager scoreManager;



    /**
     * 跳过评价：JOINED → SKIPPED，从 PENDING tab 移除
     */
    @Transactional
    public void skipReview(Meetup meetup, String userId) {
        registrationRepository.toSkipped(userId, meetup.getMeetupId());
    }

    /**
     * 提交评价（独立事务，调用 gateway 完成 diff 落库）
     * 所有待评价对象均已有评价记录时，将 registration 状态从 JOINED → REVIEWED
     */
    @Transactional
    public void submitReviewItems(Meetup meetup, String userId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {
        recapRepository.submitReviewItems(meetup.getMeetupId(), userId, toUserId, targetReviews);
        if (meetup.hasReview(userId)) {
            return;
        }
        List<String> waitlistIds = meetup.getReviewWaitlistIds(userId);
        if (waitlistIds.isEmpty()) {
            registrationRepository.toReviewed(userId, meetup.getMeetupId());
            return;
        }
        Set<String> reviewedIds = recapRepository.listReviewsByMeetupAndFrom(meetup.getMeetupId(), userId)
                .stream().map(ReviewData::getToUserId).collect(Collectors.toSet());
        if (reviewedIds.containsAll(waitlistIds)) {
            registrationRepository.toReviewed(userId, meetup.getMeetupId());
        }
    }

    /**
     * 新增比分（一次一盘）
     */
    public void addScoreItem(Meetup meetup, String userId, ScoreAddCmd cmd, LocalDateTime meetupDate, String venueName) {
        recapRepository.addScore(meetup.getMeetupId(), userId, cmd, meetupDate, venueName);
        // 触发评分重算 TODO
    }

    /**
     * 修改比分（一次一盘，bizId 定位 + version 乐观锁）
     */
    public void updateScoreItem(Meetup meetup, String userId, ScoreUpdateCmd cmd, LocalDateTime meetupDate, String venueName) {
        recapRepository.updateScore(meetup.getMeetupId(), userId, cmd, meetupDate, venueName);
        // 触发评分重算 TODO
    }

    /**
     * 删除比分（一次一盘，bizId 定位）
     */
    public void deleteScoreItem(Meetup meetup, String bizId) {
        recapRepository.deleteScore(meetup.getMeetupId(), bizId);
        // 触发评分重算 TODO
    }

    // ==================== 查询 ====================

    /**
     * 查询某场某人提交的所有评价
     */
    public List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId) {
        return recapRepository.listReviewsByMeetupAndFrom(meetupId, fromUserId);
    }

    /**
     * 查询该活动的所有比分记录
     */
    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        return recapRepository.listScoresByMeetup(meetupId);
    }

    /**
     * 查询某用户参与的所有比分记录（按比赛日期倒序）
     */
    public List<ScoreRecordData> listScoresByUserId(String userId) {
        return recapRepository.listScoresByUserId(userId);
    }
}
