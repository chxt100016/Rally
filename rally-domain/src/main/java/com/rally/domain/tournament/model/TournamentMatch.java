package com.rally.domain.tournament.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.tournament.enums.*;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 比赛聚合根：一场比赛 + 其参与者（匹配产出，落地后状态为 MATCHED 或 BOOKING，后续流转属模块4）
 */
@Getter
public class TournamentMatch {

    private final TournamentMatchData data;

    private final List<MatchParticipantData> participants;

    public TournamentMatch(TournamentMatchData data, List<MatchParticipantData> participants) {
        this.data = data;
        this.participants = participants;
    }

    public String getMatchId() {
        return this.data.getBizId();
    }

    /**
     * 由匹配分组创建比赛：判定初始状态（完美情况直接 BOOKING，否则 MATCHED），生成参与者记录
     */
    public static TournamentMatch createFromGroup(String tournamentId, int matchNo, TournamentRoundEnum round, int groupSize, List<TournamentEntryData> candidates) {
        LocalDateTime now = LocalDateTime.now();

        TournamentMatchData data = new TournamentMatchData();
        data.setBizId(IdWorker.getIdStr());
        data.setTournamentId(tournamentId);
        data.setMatchNo(matchNo);
        data.setRound(round);
        data.setGroupSize(groupSize);
        data.setMatchedTime(now);
        data.setVersion(0);

        List<TournamentEntryData> canBookers = candidates.stream()
                .filter(c -> c.getCourtAbility() == CourtAbilityEnum.CAN_BOOK)
                .collect(Collectors.toList());
        if (canBookers.size() == 1) {
            String courtBookerId = canBookers.get(0).getUserId();
            data.setStatus(TournamentMatchStatusEnum.BOOKING);
            data.setCourtBookerId(courtBookerId);
            data.setCourtBookerSelectedTime(now);
        } else {
            data.setStatus(TournamentMatchStatusEnum.MATCHED);
        }

        List<MatchParticipantData> participants = candidates.stream()
                .map(candidate -> toParticipant(data.getBizId(), tournamentId, candidate))
                .collect(Collectors.toList());

        return new TournamentMatch(data, participants);
    }

    private static MatchParticipantData toParticipant(String matchId, String tournamentId, TournamentEntryData candidate) {
        MatchParticipantData participant = new MatchParticipantData();
        participant.setBizId(IdWorker.getIdStr());
        participant.setMatchId(matchId);
        participant.setTournamentId(tournamentId);
        participant.setUserId(candidate.getUserId());
        participant.setEntryNo(candidate.getEntryNo());
        participant.setConfirmStatus(ConfirmStatusEnum.PENDING);
        participant.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
        return participant;
    }

    public void selectCourtBooker(String userId) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.MATCHED, BizErrorCode.TOURNAMENT_COURT_BOOKER_ALREADY_SELECTED);
        boolean isCandidate = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
        Assert.isTrue(isCandidate, BizErrorCode.TOURNAMENT_INVALID_COURT_BOOKER);

        LocalDateTime now = LocalDateTime.now();
        data.setCourtBookerId(userId);
        data.setCourtBookerSelectedTime(now);
        data.setStatus(TournamentMatchStatusEnum.BOOKING);
    }

    /** 尚未选出订场人时，等待达到配置时长后，参赛者可以拒绝比赛。 */
    public void rejectOnAwaitCourtBookerSelect(String userId, ScheduleRejectReasonEnum rejectReason) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.MATCHED, BizErrorCode.TOURNAMENT_NO_BOOKER_REJECT_FORBIDDEN);
        Assert.notNull(rejectReason, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);

        MatchParticipantData participant = participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        rejectMatch(participant, rejectReason, data.getMatchedTime());
    }

    /** 等待达到配置时长后，订场人可以拒绝比赛。 */
    public void rejectOnAwaitBooking(String userId, ScheduleRejectReasonEnum rejectReason) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.BOOKING, BizErrorCode.TOURNAMENT_BOOKING_REJECT_FORBIDDEN);
        Assert.eq(data.getCourtBookerId(), userId, BizErrorCode.TOURNAMENT_NOT_COURT_BOOKER);
        Assert.notNull(rejectReason, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);

        MatchParticipantData participant = participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        rejectMatch(participant, rejectReason, getBookingStageStartedAt());
    }

    /** 等待达到配置时长后，非订场人可以拒绝继续等待对方订场。 */
    public void rejectOnAwaitBookingOpponent(String userId, ScheduleRejectReasonEnum rejectReason) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.BOOKING, BizErrorCode.TOURNAMENT_WAITING_BOOKING_REJECT_FORBIDDEN);
        Assert.notNull(rejectReason, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
        Assert.notNull(data.getCourtBookerId(), BizErrorCode.TOURNAMENT_WAITING_BOOKING_REJECT_FORBIDDEN);
        Assert.isTrue(!data.getCourtBookerId().equals(userId), BizErrorCode.TOURNAMENT_WAITING_BOOKING_REJECT_FORBIDDEN);

        MatchParticipantData participant = participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        rejectMatch(participant, rejectReason, getBookingStageStartedAt());
    }

    private LocalDateTime getBookingStageStartedAt() {
        LocalDateTime bookingStageStartedAt = data.getCourtBookerSelectedTime();
        if (data.getLastRebookTime() != null
                && (bookingStageStartedAt == null || data.getLastRebookTime().isAfter(bookingStageStartedAt))) {
            bookingStageStartedAt = data.getLastRebookTime();
        }
        return bookingStageStartedAt;
    }

    private void rejectMatch(MatchParticipantData participant, ScheduleRejectReasonEnum rejectReason, LocalDateTime stageStartedAt) {
        Assert.notNull(stageStartedAt, BizErrorCode.TOURNAMENT_MATCH_REJECT_TOO_EARLY);
        int timeoutHours = SystemConfig.getInt(SystemConfigKey.TOURNAMENT_MATCH_REJECT_TIMEOUT_HOURS.getKey());
        LocalDateTime now = LocalDateTime.now();
        Assert.isTrue(!now.isBefore(stageStartedAt.plusHours(timeoutHours)), BizErrorCode.TOURNAMENT_MATCH_REJECT_TOO_EARLY);

        participant.setConfirmStatus(ConfirmStatusEnum.REJECTED);
        participant.setConfirmTime(now);
        data.setStatus(TournamentMatchStatusEnum.REJECTED);
        data.setRejectReasonCode(rejectReason.getCode());
    }

    /**
     * 提交赛约（订场）：BOOKING -> SCHEDULED，记录提交时间。
     * 场地/时间等约球数据不再落在比赛上，统一存于关联的草稿约球（meetupId），此处仅做状态流转。
     */
    public void submitBooking(String userId) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.BOOKING, BizErrorCode.TOURNAMENT_INVALID_SCHEDULE_CONFIRM);
        Assert.eq(data.getCourtBookerId(), userId, BizErrorCode.TOURNAMENT_NOT_COURT_BOOKER);

        LocalDateTime now = LocalDateTime.now();
        data.setScheduleSubmittedTime(now);
        data.setStatus(TournamentMatchStatusEnum.SCHEDULED);

        participants.forEach(p -> {
            if (p.getUserId().equals(userId)) {
                p.setConfirmStatus(ConfirmStatusEnum.CONFIRMED);
                p.setConfirmTime(now);
            } else {
                p.setConfirmStatus(ConfirmStatusEnum.PENDING);
                p.setConfirmTime(null);
            }
        });
    }

    public void confirmSchedule(String userId, boolean confirm, ScheduleRejectReasonEnum rejectReason, RebookReasonEnum rebookReason, int qualifierRejectLimit, int mainDrawRejectLimit, TournamentEntryStageEnum userStage, int userRejectCount) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.SCHEDULED, BizErrorCode.TOURNAMENT_INVALID_SCHEDULE_CONFIRM);

        MatchParticipantData participant = participants.stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        LocalDateTime now = LocalDateTime.now();

        if (!confirm) {
            Assert.isTrue((rejectReason != null) ^ (rebookReason != null), BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
        }

        if (!confirm && rejectReason != null) {
            int limit = userStage == TournamentEntryStageEnum.QUALIFY ? qualifierRejectLimit : mainDrawRejectLimit;
            Assert.isTrue(userRejectCount < limit, BizErrorCode.TOURNAMENT_REJECT_LIMIT_REACHED);
            participant.setConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.REJECTED);
            data.setRejectReasonCode(rejectReason.getCode());
        } else if (!confirm && rebookReason != null) {
            Assert.notNull(rebookReason, BizErrorCode.TOURNAMENT_REBOOK_REASON_REQUIRED);
            participant.setConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.BOOKING);
            data.setLastRebookBy(userId);
            data.setLastRebookReasonCode(rebookReason.getCode());
            data.setLastRebookTime(now);

            participants.forEach(p -> {
                p.setConfirmStatus(ConfirmStatusEnum.PENDING);
                p.setConfirmTime(null);
            });
        } else {
            participant.setConfirmStatus(ConfirmStatusEnum.CONFIRMED);
            participant.setConfirmTime(now);

            boolean allConfirmed = participants.stream().allMatch(p -> p.getConfirmStatus() == ConfirmStatusEnum.CONFIRMED);
            if (allConfirmed) {
                data.setStatus(TournamentMatchStatusEnum.PENDING_PLAY);
            }
        }
    }

    public void submitResult(String userId, Integer winnerEntryNo) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.PENDING_PLAY, BizErrorCode.TOURNAMENT_INVALID_RESULT_SUBMIT);
        Assert.notNull(winnerEntryNo, BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);

        boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
        Assert.isTrue(isParticipant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        boolean winnerIsParticipant = participants.stream().anyMatch(p -> winnerEntryNo.equals(p.getEntryNo()));
        Assert.isTrue(winnerIsParticipant, BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);

        LocalDateTime now = LocalDateTime.now();
        data.setWinnerEntryNo(winnerEntryNo);
        data.setSubmittedTime(now);
        data.setSubmitterUserId(userId);
        data.setStatus(TournamentMatchStatusEnum.PENDING_CONFIRM);

        participants.forEach(p -> {
            if (p.getUserId().equals(userId)) {
                p.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
                p.setResultConfirmTime(now);
            } else {
                p.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
                p.setResultConfirmTime(null);
            }
        });
    }

    public void confirmResult(String userId, boolean confirm, ResultRejectReasonEnum rejectReason, int qualifierRejectLimit, int mainDrawRejectLimit, TournamentEntryStageEnum userStage, int userRejectCount) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.PENDING_CONFIRM, BizErrorCode.TOURNAMENT_INVALID_RESULT_CONFIRM);

        MatchParticipantData participant = participants.stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        LocalDateTime now = LocalDateTime.now();

        if (!confirm) {
            int limit = userStage == TournamentEntryStageEnum.QUALIFY ? qualifierRejectLimit : mainDrawRejectLimit;
            Assert.isTrue(userRejectCount < limit, BizErrorCode.TOURNAMENT_REJECT_LIMIT_REACHED);
            Assert.notNull(rejectReason, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
            // 拒绝结果即拒绝比赛：比赛终止（REJECTED），保留已提交的比分/胜负等记录供追溯，不做回退重报
            participant.setResultConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setResultConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.REJECTED);
            data.setRejectReasonCode(rejectReason.getCode());
        } else {
            participant.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
            participant.setResultConfirmTime(now);

            boolean allConfirmed = participants.stream().allMatch(p -> p.getResultConfirmStatus() == ConfirmStatusEnum.CONFIRMED);
            if (allConfirmed) {
                Assert.notNull(data.getWinnerEntryNo(), BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);
                data.setStatus(TournamentMatchStatusEnum.COMPLETED);
                data.setCompletedTime(now);
            }
        }
    }
}
