package com.rally.domain.score;

import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.review.gateway.ReviewGateway;
import com.rally.domain.review.gateway.ScoreRecordGateway;
import com.rally.domain.score.gateway.ScoreStatusGateway;
import com.rally.domain.score.model.*;
import com.rally.domain.score.strategy.CalibrationStrategy;
import com.rally.domain.score.strategy.CredibilityStrategy;
import com.rally.domain.score.strategy.ReputationStrategy;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.user.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.ProfileChangeLogData;
import com.rally.domain.user.model.TennisProfileData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评分管理器实现（事务入口）
 * 编排策略计算 → 写 profile → 记 change_log → 推进核查期 → 维护 status
 */
@Slf4j
@Service
public class ScoreManager {

    @Resource
    private ReputationStrategy reputationStrategy;
    @Resource
    private CredibilityStrategy credibilityStrategy;
    @Resource
    private CalibrationStrategy calibrationStrategy;

    @Resource
    private MeetupGateway meetupGateway;
    @Resource
    private ReviewGateway reviewGateway;
    @Resource
    private ScoreRecordGateway scoreRecordGateway;
    @Resource
    private TennisProfileGateway profileGateway;
    @Resource
    private ProfileChangeLogGateway changeLogGateway;
    @Resource
    private ScoreStatusGateway statusGateway;


    public void recalc(String meetupId) {
        log.info("开始全量重算评分，meetupId={}", meetupId);

        // 1. 组装上下文
        ScoreContext ctx = buildContext(meetupId);

        // 2. 逐参与者、逐维度计算三维分
        for (String userId : ctx.getParticipants()) {
            // 新人保护：收到评价数 < min_reviews 则不进实时计算
            if (isNewbie(userId)) {
                log.debug("用户 {} 为新人，跳过三维计算", userId);
                continue;
            }

            ScoreResult result = new ScoreResult(userId);

            // 计算三维分
            ScoreChange repChange = reputationStrategy.calculate(ctx, userId);
            result.put(repChange);

            ScoreChange credChange = credibilityStrategy.calculate(ctx, userId);
            result.put(credChange);

            ScoreChange calChange = calibrationStrategy.calculate(ctx, userId);
            result.put(calChange);



            // 4. 写 profile 三维分/total/level（经 01 Gateway）
            profileGateway.updateScoreFields(userId,
                    result.getReputation(),
                    result.getCredibility(),
                    result.getCalibration(),
                    result.getIsNewbie());

            // 5. 记三维分变更日志（仅 before!=after 时记）
            for (ScoreChange change : result.changedDimensions()) {
                saveChangeLog(userId, change);
            }

            // 6. 检查核查期解除/重置
            handleReviewPeriod(ctx, userId);
        }

        // 7. 维护幂等状态
        statusGateway.markProcessed(meetupId);

        log.info("全量重算评分完成，meetupId={}", meetupId);
    }


    public void applyReputationPenalty(ReputationPenaltyCmd cmd) {
        log.info("执行单点信誉分扣减，userId={}, reason={}, refMeetupId={}",
                cmd.getUserId(), cmd.getReason(), cmd.getRefMeetupId());

        // 1. 读当前信誉分
        BigDecimal before = profileGateway.findByUserId(cmd.getUserId())
                .map(TennisProfileData::getReputationScore)
                .orElse(BigDecimal.valueOf(100));

        // 2. 按原因码查配置扣分值
        int delta = resolvePenalty(cmd.getReason());

        // 3. 计算新分（下限 0）
        int minScore = SystemConfig.getInt("score.reputation.min", 0);
        BigDecimal after = before.add(BigDecimal.valueOf(delta));
        after = after.max(BigDecimal.valueOf(minScore));

        // 4. 重算总分/等级
        ScoreResult result = recomputeTotalOnly(cmd.getUserId(), after);

        // 5. 写 profile
        profileGateway.updateScoreFields(cmd.getUserId(),
                result.getReputation(),
                result.getCredibility(),
                result.getCalibration(),
                result.getIsNewbie());

        // 6. 记日志
        ProfileChangeLogData logData = new ProfileChangeLogData();
        logData.setUserId(cmd.getUserId());
        logData.setType(ChangeLogTypeEnum.REPUTATION);
        logData.setBeforeValue(before);
        logData.setAfterValue(after);
        logData.setValue(BigDecimal.valueOf(delta));
        logData.setReason(cmd.getReason());
        logData.setRefId(cmd.getRefMeetupId());
        logData.setRemark("单点扣分: " + cmd.getReason());
        changeLogGateway.save(logData);

        log.info("单点信誉分扣减完成，userId={}, before={}, after={}, delta={}",
                cmd.getUserId(), before, after, delta);
    }

    /**
     * 组装计算上下文
     */
    private ScoreContext buildContext(String meetupId) {
        ScoreContext ctx = new ScoreContext();
        ctx.setMeetupId(meetupId);
        ctx.setParticipants(meetupGateway.listParticipantUserIds(meetupId));
        ctx.setReviewGateway(reviewGateway);
        ctx.setScoreRecordGateway(scoreRecordGateway);
        ctx.setMeetupGateway(meetupGateway);
        ctx.setProfileGateway(profileGateway);
        ctx.setChangeLogGateway(changeLogGateway);
        return ctx;
    }

    /**
     * 判断是否新人（收到评价数 < min_reviews）
     */
    private boolean isNewbie(String userId) {
        int minReviews = SystemConfig.getInt("score.newbie.min_reviews", 3);
        // TODO: 需要统计收到的 ntrp_vote 去重场次数
        // 暂时从 profile 读取 isNewbie 字段
        return profileGateway.findByUserId(userId)
                .map(TennisProfileData::getIsNewbie)
                .orElse(true);
    }

    /**
     * 保存变更日志
     */
    private void saveChangeLog(String userId, ScoreChange change) {
        ProfileChangeLogData logData = new ProfileChangeLogData();
        logData.setUserId(userId);
        logData.setType(ChangeLogTypeEnum.valueOf(change.getDimension().getValue().toUpperCase()));
        logData.setBeforeValue(change.getBefore());
        logData.setAfterValue(change.getAfter());
        logData.setValue(change.getValue());
        logData.setReason(change.getReason());
        logData.setRefId(change.getRefId());
        logData.setRemark(change.getRemark());
        changeLogGateway.save(logData);
    }

    /**
     * 处理核查期（推进/重置/解除）
     */
    private void handleReviewPeriod(ScoreContext ctx, String userId) {
        // TODO: 实现核查期逻辑
        // 1. 检查是否处于核查期
        // 2. 检查本场是否有「差」票
        // 3. 推进/重置进度
        // 4. 检查是否达解除条件
    }

    /**
     * 仅重算总分/等级（单点扣分用）
     */
    private ScoreResult recomputeTotalOnly(String userId, BigDecimal newReputation) {
        ScoreResult result = new ScoreResult(userId);
        result.setReputation(newReputation);

        // 读取当前可信度和校准度
        profileGateway.findByUserId(userId).ifPresent(profile -> {
            result.setCredibility(profile.getCredibilityScore());
            result.setCalibration(profile.getCalibrationScore());
        });


        return result;
    }

    /**
     * 按原因码解析扣分值
     */
    private int resolvePenalty(ChangeReasonEnum reason) {
        return switch (reason) {
            case CANCEL_24H_OUT -> SystemConfig.getInt("meetup.cancel.penalty_24h_out", -5);
            case CANCEL_12_24H -> SystemConfig.getInt("meetup.cancel.penalty_12_24h", -10);
            case CANCEL_6_12H -> SystemConfig.getInt("meetup.cancel.penalty_6_12h", -15);
            case CANCEL_UNDER_6H -> SystemConfig.getInt("meetup.cancel.penalty_under_6h", -25);
            case QUIT_UNDER_6H -> SystemConfig.getInt("meetup.quit.penalty_under_6h", -25);
            case NO_SHOW -> SystemConfig.getInt("score.reputation.no_show", -25);
            case LATE -> SystemConfig.getInt("score.reputation.late", -10);
            default -> 0;
        };
    }
}
