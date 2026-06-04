package com.rally.review;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.review.enums.SetFormatEnum;
import com.rally.domain.review.gateway.ScoreRecordGateway;
import com.rally.domain.review.model.ScoreRecordCmd;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.score.gateway.ScoreManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 比分编排服务：记/改盘、删盘、查比分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreRecordAppService {

    private final ScoreRecordGateway scoreRecordGateway;
    private final MeetupGateway meetupGateway;
    private final ScoreManager scoreManager;

    /**
     * 记比分（新增/修改盘）
     * bizId 为空 = 新增，bizId 非空 = 修改（乐观锁）
     */
    @Transactional
    public ScoreRecordData saveScore(ScoreRecordCmd cmd) {
        String operatorId = UserContext.get();
        String rallyMeetupId = cmd.getRallyMeetupId();

        // 1. 约球是否存在
        MeetupData meetup = meetupGateway.findByBizId(rallyMeetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 约球是否已结束
        if (!meetupGateway.isFinished(rallyMeetupId)) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FINISHED);
        }

        // 3. 操作者是否为参与者
        if (!meetupGateway.isParticipant(rallyMeetupId, operatorId)) {
            throw new BusinessException(BizErrorCode.REVIEW_NOT_PARTICIPANT);
        }

        // 4. 比分选手是否为参与者
        validatePlayers(rallyMeetupId, cmd);

        // 5. 赛制比分合法性校验
        validateScore(cmd.getSetFormat(), cmd.getSideAScore(), cmd.getSideBScore());

        // 6. 新增 vs 修改
        if (cmd.getBizId() == null || cmd.getBizId().isEmpty()) {
            // 新增：检查盘号是否已存在
            List<ScoreRecordData> existing = scoreRecordGateway.listByMeetupId(rallyMeetupId);
            boolean setExists = existing.stream()
                    .anyMatch(s -> s.getSetNumber().equals(cmd.getSetNumber()));
            if (setExists) {
                throw new BusinessException(BizErrorCode.SCORE_SET_DUPLICATE);
            }
            return insertScore(cmd, operatorId);
        } else {
            // 修改：乐观锁更新
            return updateScore(cmd, operatorId);
        }
    }

    /**
     * 删除某盘比分（乐观锁）
     */
    @Transactional
    public void deleteScore(String bizId, Integer version) {
        String operatorId = UserContext.get();

        // 查询比分记录
        ScoreRecordData existing = scoreRecordGateway.findByBizId(bizId);
        if (existing == null) {
            throw new BusinessException(BizErrorCode.DATA_NOT_FOUND);
        }

        // 参与者校验
        if (!meetupGateway.isParticipant(existing.getRallyMeetupId(), operatorId)) {
            throw new BusinessException(BizErrorCode.REVIEW_NOT_PARTICIPANT);
        }

        // 乐观锁删除
        int affected = scoreRecordGateway.deleteByBizIdWithLock(bizId, version);
        if (affected == 0) {
            throw new BusinessException(BizErrorCode.SCORE_VERSION_CONFLICT);
        }

        // 触发评分重算
        try {
            scoreManager.recalc(existing.getRallyMeetupId());
        } catch (Exception e) {
            log.error("删除盘触发评分重算失败，meetupId={}", existing.getRallyMeetupId(), e);
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "评分计算异常，请重试");
        }
    }

    /**
     * 获取某场比分（按 set_number 升序）
     */
    public List<ScoreRecordData> listScores(String rallyMeetupId) {
        return scoreRecordGateway.listByMeetupId(rallyMeetupId);
    }

    // ==================== 内部方法 ====================

    /**
     * 新增盘记录
     */
    private ScoreRecordData insertScore(ScoreRecordCmd cmd, String operatorId) {
        ScoreRecordData data = new ScoreRecordData();
        data.setRallyMeetupId(cmd.getRallyMeetupId());
        data.setSetNumber(cmd.getSetNumber());
        data.setSetFormat(cmd.getSetFormat());
        data.setSideAPlayer1(cmd.getSideAPlayer1());
        data.setSideAPlayer2(cmd.getSideAPlayer2());
        data.setSideBPlayer1(cmd.getSideBPlayer1());
        data.setSideBPlayer2(cmd.getSideBPlayer2());
        data.setSideAScore(cmd.getSideAScore());
        data.setSideBScore(cmd.getSideBScore());
        data.setRecordedBy(operatorId);
        data.setVersion(0);

        scoreRecordGateway.insert(data);

        // 触发评分重算
        try {
            scoreManager.recalc(cmd.getRallyMeetupId());
        } catch (Exception e) {
            log.error("新增盘触发评分重算失败，meetupId={}", cmd.getRallyMeetupId(), e);
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "评分计算异常，请重试");
        }

        return data;
    }

    /**
     * 乐观锁更新盘记录
     */
    private ScoreRecordData updateScore(ScoreRecordCmd cmd, String operatorId) {
        ScoreRecordData existing = scoreRecordGateway.findByBizId(cmd.getBizId());
        if (existing == null) {
            throw new BusinessException(BizErrorCode.DATA_NOT_FOUND);
        }

        // 更新字段
        existing.setSetFormat(cmd.getSetFormat());
        existing.setSideAPlayer1(cmd.getSideAPlayer1());
        existing.setSideAPlayer2(cmd.getSideAPlayer2());
        existing.setSideBPlayer1(cmd.getSideBPlayer1());
        existing.setSideBPlayer2(cmd.getSideBPlayer2());
        existing.setSideAScore(cmd.getSideAScore());
        existing.setSideBScore(cmd.getSideBScore());
        existing.setRecordedBy(operatorId);
        // version 保持原值，由乐观锁插件自动 +1

        int affected = scoreRecordGateway.updateWithLock(existing);
        if (affected == 0) {
            throw new BusinessException(BizErrorCode.SCORE_VERSION_CONFLICT);
        }

        // 触发评分重算
        try {
            scoreManager.recalc(cmd.getRallyMeetupId());
        } catch (Exception e) {
            log.error("更新盘触发评分重算失败，meetupId={}", cmd.getRallyMeetupId(), e);
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "评分计算异常，请重试");
        }

        return existing;
    }

    /**
     * 校验比分选手是否为参与者，以及单打/双打合法性
     */
    private void validatePlayers(String rallyMeetupId, ScoreRecordCmd cmd) {
        // A1 和 B1 必填
        if (!meetupGateway.isParticipant(rallyMeetupId, cmd.getSideAPlayer1())) {
            throw new BusinessException(BizErrorCode.SCORE_PLAYER_INVALID, "A侧选手1不是参与者");
        }
        if (!meetupGateway.isParticipant(rallyMeetupId, cmd.getSideBPlayer1())) {
            throw new BusinessException(BizErrorCode.SCORE_PLAYER_INVALID, "B侧选手1不是参与者");
        }
        // player2 有值时也校验
        if (cmd.getSideAPlayer2() != null && !cmd.getSideAPlayer2().isEmpty()) {
            if (!meetupGateway.isParticipant(rallyMeetupId, cmd.getSideAPlayer2())) {
                throw new BusinessException(BizErrorCode.SCORE_PLAYER_INVALID, "A侧选手2不是参与者");
            }
        }
        if (cmd.getSideBPlayer2() != null && !cmd.getSideBPlayer2().isEmpty()) {
            if (!meetupGateway.isParticipant(rallyMeetupId, cmd.getSideBPlayer2())) {
                throw new BusinessException(BizErrorCode.SCORE_PLAYER_INVALID, "B侧选手2不是参与者");
            }
        }
    }

    /**
     * 赛制比分合法性校验
     */
    private void validateScore(SetFormatEnum format, int scoreA, int scoreB) {
        // 不允许平局
        if (scoreA == scoreB) {
            throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID, "网球单盘必分胜负，不允许平局");
        }
        // 不允许负分
        if (scoreA < 0 || scoreB < 0) {
            throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID, "比分不能为负");
        }

        int winner = Math.max(scoreA, scoreB);
        int loser = Math.min(scoreA, scoreB);

        switch (format) {
            case GAMES_4: {
                int maxGames = SystemConfig.getInt("review.score.games4_max", 5);
                if (winner > maxGames) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID,
                            "4局制单侧最大" + maxGames + "局");
                }
                // 胜方 = 4 或 5
                if (winner < 4) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID, "4局制胜方至少4局");
                }
                break;
            }
            case GAMES_6: {
                int maxGames = SystemConfig.getInt("review.score.games6_max", 7);
                if (winner > maxGames) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID,
                            "6局制单侧最大" + maxGames + "局");
                }
                // 胜方 = 6 或 7
                if (winner < 6) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID, "6局制胜方至少6局");
                }
                // 7 仅在 7:5 / 7:6 出现
                if (winner == 7 && loser < 5) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID, "7局仅允许7:5或7:6");
                }
                break;
            }
            case TIEBREAK: {
                int minScore = SystemConfig.getInt("review.score.tiebreak_min", 7);
                int minLead = SystemConfig.getInt("review.score.tiebreak_lead", 2);
                if (winner < minScore) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID,
                            "抢七胜方至少" + minScore + "分");
                }
                if (winner - loser < minLead) {
                    throw new BusinessException(BizErrorCode.SCORE_FORMAT_INVALID,
                            "抢七至少领先" + minLead + "分");
                }
                break;
            }
        }
    }
}
