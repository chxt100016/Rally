package com.rally.domain.tournament.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
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
        participant.setTeamId(teamIdOf(candidate));
        participant.setConfirmStatus(ConfirmStatusEnum.PENDING);
        participant.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
        return participant;
    }

    /** 双打同队两人共用同一个 teamId（取两人 userId 中较小的一个，保证同队双方算出一致的值） */
    private static String teamIdOf(TournamentEntryData candidate) {
        if (candidate.getPartnerId() == null) {
            return null;
        }
        return candidate.getUserId().compareTo(candidate.getPartnerId()) <= 0 ? candidate.getUserId() : candidate.getPartnerId();
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

    public void giveUpCourtBooker(String userId) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.BOOKING, BizErrorCode.TOURNAMENT_INVALID_SCHEDULE_CONFIRM);
        Assert.eq(data.getCourtBookerId(), userId, BizErrorCode.TOURNAMENT_NOT_COURT_BOOKER);

        data.setCourtBookerId(null);
        data.setCourtBookerSelectedTime(null);
        data.setStatus(TournamentMatchStatusEnum.MATCHED);
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

    public void confirmSchedule(String userId, boolean confirm, ScheduleRejectReasonEnum rejectReason, String rejectReasonText, RebookReasonEnum rebookReason, String rebookReasonText, int qualifierRejectLimit, int mainDrawRejectLimit, TournamentEntryStageEnum userStage, int userRejectCount) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.SCHEDULED, BizErrorCode.TOURNAMENT_INVALID_SCHEDULE_CONFIRM);

        MatchParticipantData participant = participants.stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        LocalDateTime now = LocalDateTime.now();

        if (!confirm && rejectReason != null) {
            int limit = userStage == TournamentEntryStageEnum.QUALIFY ? qualifierRejectLimit : mainDrawRejectLimit;
            Assert.isTrue(userRejectCount < limit, BizErrorCode.TOURNAMENT_REJECT_LIMIT_REACHED);
            Assert.isTrue(rejectReason == ScheduleRejectReasonEnum.TIME_PLACE_CONFLICT || rejectReason == ScheduleRejectReasonEnum.DONT_WANT_PLAY || rejectReason == ScheduleRejectReasonEnum.OTHER, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
            if (rejectReason == ScheduleRejectReasonEnum.OTHER) {
                Assert.notBlank(rejectReasonText, BizErrorCode.PARAM_ERROR);
            }

            participant.setConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.REJECTED);
            data.setRejectReason(rejectReason.getCode() + (rejectReasonText != null ? ":" + rejectReasonText : ""));
        } else if (!confirm && rebookReason != null) {
            Assert.notNull(rebookReason, BizErrorCode.TOURNAMENT_REBOOK_REASON_REQUIRED);
            Assert.isTrue(rebookReason == RebookReasonEnum.TIME_NOT_SUITABLE || rebookReason == RebookReasonEnum.PLACE_NOT_SUITABLE || rebookReason == RebookReasonEnum.OTHER, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
            if (rebookReason == RebookReasonEnum.OTHER) {
                Assert.notBlank(rebookReasonText, BizErrorCode.PARAM_ERROR);
            }

            participant.setConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.BOOKING);
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

    public void submitResult(String userId, List<String> winnerUserIds) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.PENDING_PLAY, BizErrorCode.TOURNAMENT_INVALID_RESULT_SUBMIT);
        Assert.notNull(winnerUserIds, BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);
        Assert.isTrue(!winnerUserIds.isEmpty(), BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);

        boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
        Assert.isTrue(isParticipant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        LocalDateTime now = LocalDateTime.now();
        data.setSubmittedTime(now);
        data.setSubmitterUserId(userId);
        data.setStatus(TournamentMatchStatusEnum.PENDING_CONFIRM);

        participants.forEach(p -> {
            boolean isWinner = winnerUserIds.contains(p.getUserId());
            p.setIsWinner(isWinner);
            p.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
            p.setResultConfirmTime(null);
        });
    }

    public void confirmResult(String userId, boolean confirm, ResultRejectReasonEnum rejectReason, String rejectReasonText, int qualifierRejectLimit, int mainDrawRejectLimit, TournamentEntryStageEnum userStage, int userRejectCount) {
        Assert.eq(data.getStatus(), TournamentMatchStatusEnum.PENDING_CONFIRM, BizErrorCode.TOURNAMENT_INVALID_RESULT_CONFIRM);

        MatchParticipantData participant = participants.stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
        Assert.notNull(participant, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        LocalDateTime now = LocalDateTime.now();

        if (!confirm) {
            int limit = userStage == TournamentEntryStageEnum.QUALIFY ? qualifierRejectLimit : mainDrawRejectLimit;
            Assert.isTrue(userRejectCount < limit, BizErrorCode.TOURNAMENT_REJECT_LIMIT_REACHED);
            Assert.notNull(rejectReason, BizErrorCode.TOURNAMENT_INVALID_REJECT_REASON);
            if (rejectReason == ResultRejectReasonEnum.OTHER) {
                Assert.notBlank(rejectReasonText, BizErrorCode.PARAM_ERROR);
            }

            participant.setResultConfirmStatus(ConfirmStatusEnum.REJECTED);
            participant.setResultConfirmTime(now);
            data.setStatus(TournamentMatchStatusEnum.PENDING_PLAY);
            data.setSubmittedTime(null);
            data.setSubmitterUserId(null);
            data.setRejectReason(rejectReason.getCode() + (rejectReasonText != null ? ":" + rejectReasonText : ""));

            participants.forEach(p -> {
                p.setIsWinner(null);
                p.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
                p.setResultConfirmTime(null);
            });
        } else {
            participant.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
            participant.setResultConfirmTime(now);

            boolean allConfirmed = participants.stream().allMatch(p -> p.getResultConfirmStatus() == ConfirmStatusEnum.CONFIRMED);
            if (allConfirmed) {
                data.setStatus(TournamentMatchStatusEnum.COMPLETED);
            }
        }
    }
}

