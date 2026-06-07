package com.rally.domain.recap.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.recap.enums.RecapTypeEnum;
import com.rally.domain.recap.gateway.RecapGateway;
import com.rally.domain.recap.model.*;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.score.ScoreManager;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    private final UserGateway userGateway;
    private final ScoreManager scoreManager;

    /**
     * 加载 Recap 聚合根（含业务校验）
     */
    public Recap get(String userId, String meetupId) {
        // 1. 活动是否存在
        MeetupData meetup = recapGateway.findMeetupById(meetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 活动是否已结束（懒判定：end_time < NOW()）
        if (meetup.getEndTime() != null && meetup.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FINISHED);
        }

        // 3. 截止时间校验
        int deadlineDays = SystemConfig.getInt("review.deadline_days", 30);
        LocalDateTime deadlineAt = meetup.getEndTime().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BusinessException(BizErrorCode.REVIEW_DEADLINE_PASSED);
        }

        // 4. 获取参与人列表
        List<String> participantIds = recapGateway.listParticipantUserIds(meetupId);
        List<RegistrationData> participants = participantIds.stream()
                .map(uid -> {
                    RegistrationData rd = new RegistrationData();
                    rd.setUserId(uid);
                    return rd;
                })
                .toList();

        // 5. 获取当前用户的评价
        List<ReviewData> myReviews = recapGateway.listMyReviews(meetupId, userId);

        // 6. 获取比分
        List<ScoreRecordData> scores = recapGateway.listScores(meetupId);

        // 7. 构建聚合根
        Recap recap = RecapFactory.build(userId, meetupId, meetup, participants, myReviews, scores);

        // 8. 校验当前用户是参与者
        recap.assertIsParticipant();

        return recap;
    }

    /**
     * 提交评价（独立事务，调用 gateway 完成 diff 落库）
     */
    @Transactional
    public void submitReviews(Recap recap, List<RecapCmd.ReviewItem> targetReviews) {
        recapGateway.submitReviews(
                recap.getMeetupId(), recap.getUserId(),
                new ArrayList<>(recap.getMyReviews().values()),
                targetReviews);
    }

    /**
     * 提交比分（独立事务，调用 gateway 完成版本校验与 diff 落库）
     *
     * @return true=保存成功，false=版本冲突（比分未保存）
     */
    @Transactional
    public boolean submitScores(Recap recap, List<RecapCmd.ScoreItem> targetScores, Integer clientVersion) {
        List<ScoreRecordData> currentScores = recap.getScoreBoard() != null
                ? recap.getScoreBoard().getScores() : null;

        // gateway 内部完成版本校验、diff 与落库，冲突抛异常
        try {
            recapGateway.submitScores(
                    recap.getMeetupId(), recap.getUserId(),
                    currentScores, targetScores, clientVersion);
        } catch (ScoreConflictException e) {
            log.warn("比分版本冲突，meetupId={}", recap.getMeetupId());
            return false;
        }

        // 触发评分重算
        try {
            scoreManager.recalc(recap.getMeetupId());
        } catch (Exception e) {
            log.error("赛后收集触发评分重算失败，meetupId={}", recap.getMeetupId(), e);
        }

        return true;
    }

    /**
     * 构建赛后收集详情 VO
     */
    public RecapDetailVO detail(Recap recap) {
        RecapDetailVO vo = new RecapDetailVO();
        MeetupData meetup = recap.getMeetup();

        // 活动信息
        vo.setMeetupId(recap.getMeetupId());
        vo.setTitle(meetup.getTitle());
        vo.setStartTime(meetup.getStartTime());
        vo.setEndTime(meetup.getEndTime());
        vo.setCourtName(meetup.getCourtName());

        // 参与人列表（排除自己）
        List<RecapDetailVO.ParticipantItem> participantItems = buildParticipants(recap);
        vo.setParticipants(participantItems);

        // 当前用户已填评价（按 toUser 分组）
        Map<String, List<RecapDetailVO.ReviewItem>> myReviewsMap = buildMyReviewsMap(recap);
        vo.setMyReviews(myReviewsMap);

        // 比分
        List<RecapDetailVO.ScoreItem> scoreItems = buildScoreItems(recap);
        vo.setScores(scoreItems);
        vo.setScoreVersion(recap.getScoreBoard() != null ? recap.getScoreBoard().getVersion() : 0);

        // 填写状态
        vo.setScoreFilled(!scoreItems.isEmpty());
        vo.setReviewFilled(isReviewFilled(participantItems));

        return vo;
    }

    // ==================== 内部方法 ====================

    private List<RecapDetailVO.ParticipantItem> buildParticipants(Recap recap) {
        String currentUserId = recap.getUserId();
        Set<String> reviewedKeys = recap.getMyReviews().keySet();
        List<RecapDetailVO.ParticipantItem> items = new ArrayList<>();

        for (RegistrationData reg : recap.getParticipants()) {
            String uid = reg.getUserId();
            if (uid.equals(currentUserId)) {
                continue;
            }

            RecapDetailVO.ParticipantItem item = new RecapDetailVO.ParticipantItem();
            item.setUserId(uid);

            UserData userData = userGateway.findByUserId(uid).orElse(null);
            if (userData != null) {
                item.setNickname(userData.getNickname());
                item.setAvatarUrl(userData.getAvatarUrl());
            }

            List<String> reviewedTypes = new ArrayList<>();
            for (RecapTypeEnum type : RecapTypeEnum.values()) {
                String key = uid + ":" + type.name();
                if (reviewedKeys.contains(key)) {
                    reviewedTypes.add(type.name());
                }
            }
            item.setReviewedTypes(reviewedTypes);

            items.add(item);
        }
        return items;
    }

    private Map<String, List<RecapDetailVO.ReviewItem>> buildMyReviewsMap(Recap recap) {
        Map<String, List<RecapDetailVO.ReviewItem>> map = new LinkedHashMap<>();
        for (ReviewData review : recap.getMyReviews().values()) {
            RecapDetailVO.ReviewItem item = new RecapDetailVO.ReviewItem();
            item.setToUserId(review.getToUserId());
            item.setType(review.getReviewType().name());
            item.setValue(review.getReviewValue());
            map.computeIfAbsent(review.getToUserId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private List<RecapDetailVO.ScoreItem> buildScoreItems(Recap recap) {
        if (recap.getScoreBoard() == null || recap.getScoreBoard().getScores() == null) {
            return new ArrayList<>();
        }
        return recap.getScoreBoard().getScores().stream()
                .map(this::toScoreItem)
                .toList();
    }

    private boolean isReviewFilled(List<RecapDetailVO.ParticipantItem> participants) {
        if (participants.isEmpty()) {
            return true;
        }
        for (RecapDetailVO.ParticipantItem p : participants) {
            List<String> reviewed = p.getReviewedTypes();
            if (reviewed == null || reviewed.size() < RecapTypeEnum.values().length) {
                return false;
            }
        }
        return true;
    }

    private RecapDetailVO.ScoreItem toScoreItem(ScoreRecordData data) {
        RecapDetailVO.ScoreItem item = new RecapDetailVO.ScoreItem();
        item.setBizId(data.getBizId());
        item.setSetNum(data.getSetNumber());
        item.setSetFormat(data.getSetFormat() != null ? data.getSetFormat().name() : null);
        item.setSideAPlayer1(data.getSideAPlayer1());
        item.setSideAPlayer2(data.getSideAPlayer2());
        item.setSideBPlayer1(data.getSideBPlayer1());
        item.setSideBPlayer2(data.getSideBPlayer2());
        item.setSideAScore(data.getSideAScore());
        item.setSideBScore(data.getSideBScore());
        return item;
    }
}
