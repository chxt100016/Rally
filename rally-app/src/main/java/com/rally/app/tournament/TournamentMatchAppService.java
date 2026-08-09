package com.rally.app.tournament;

import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotifySubscribeService;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.*;
import com.rally.domain.tournament.service.TournamentMatchFlowService;
import com.rally.notify.TournamentNotifyAssembler;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentMatchAppService {

    private final TournamentMatchFlowService matchFlowService;
    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final MeetupRepository meetupRepository;
    private final NotifySubscribeService notifySubscribeService;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> selectCourtBooker(SelectCourtBookerCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.selectCourtBooker(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> submitBooking(SubmitBookingCmd cmd) {
        String userId = UserContext.get();
        String meetupId = matchFlowService.submitBooking(cmd, userId);
        TournamentMatch match = getMatch(cmd.getMatchId());
        TournamentData tournament = getTournament(match.getData().getTournamentId());
        MeetupData booking = meetupRepository.findByBizId(meetupId);
        notifySubscribeService.notify(NotifyBizType.TOURNAMENT, tournament.getBizId(),
                NoticeScene.TOURNAMENT_BOOKING_SUBMITTED, otherParticipantIds(match, userId),
                TournamentNotifyAssembler.bookingSubmittedData(tournament.getTournamentName(), booking.getStartTime(), booking.getCourtName()));
        return Result.ok(meetupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitCourtBookerSelect(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.rejectOnAwaitCourtBookerSelect(cmd.getMatchId(), userId, cmd.getRejectReason());
        notifyRejected(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitBooking(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.rejectOnAwaitBooking(cmd.getMatchId(), userId, cmd.getRejectReason());
        notifyRejected(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitBookingOpponent(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.rejectOnAwaitBookingOpponent(cmd.getMatchId(), userId, cmd.getRejectReason());
        notifyRejected(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitOpponentScheduleConfirm(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.rejectOnAwaitOpponentScheduleConfirm(cmd.getMatchId(), userId, cmd.getRejectReason());
        notifyRejected(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmSchedule(ScheduleConfirmCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.handleScheduleConfirm(cmd.getMatchId(), userId, cmd.getConfirm(), cmd.getRejectReason(), cmd.getRebookReason());
        if (!cmd.getConfirm() && cmd.getRejectReason() != null) {
            notifyRejected(cmd.getMatchId(), userId);
        }
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitResult(SubmitResultCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.submitResult(cmd.getMatchId(), userId, cmd.getWinnerEntryNo());
        grantTournamentNotices(cmd.getMatchId(), userId, cmd.getAcceptedNoticeScenes());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmResult(ResultConfirmCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.handleResultConfirm(cmd.getMatchId(), userId, cmd.getConfirm(), cmd.getRejectReason());
        grantTournamentNotices(cmd.getMatchId(), userId, cmd.getAcceptedNoticeScenes());
        if (!cmd.getConfirm()) {
            notifyRejected(cmd.getMatchId(), userId);
        }
        return Result.ok();
    }

    private void grantTournamentNotices(String matchId, String userId, List<String> acceptedNoticeScenes) {
        TournamentMatch match = getMatch(matchId);
        notifySubscribeService.grant(userId, NotifyBizType.TOURNAMENT, match.getData().getTournamentId(),
                TournamentNotifyAssembler.parseScenes(acceptedNoticeScenes));
    }

    private void notifyRejected(String matchId, String rejecterUserId) {
        TournamentMatch match = getMatch(matchId);
        TournamentData tournament = getTournament(match.getData().getTournamentId());
        notifySubscribeService.notify(NotifyBizType.TOURNAMENT, tournament.getBizId(),
                NoticeScene.TOURNAMENT_REJECTED, otherParticipantIds(match, rejecterUserId),
                TournamentNotifyAssembler.rejectedData(tournament.getTournamentName()));
    }

    private TournamentMatch getMatch(String matchId) {
        return matchRepository.findByBizIdWithParticipants(matchId);
    }

    private TournamentData getTournament(String tournamentId) {
        return tournamentRepository.findByBizId(tournamentId);
    }

    private List<String> otherParticipantIds(TournamentMatch match, String excludedUserId) {
        return match.getParticipants().stream()
                .map(MatchParticipantData::getUserId)
                .filter(userId -> !userId.equals(excludedUserId))
                .distinct()
                .toList();
    }
}
