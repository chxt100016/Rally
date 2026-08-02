package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupFactory;
import com.rally.domain.tournament.enums.*;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentMatchFlowService {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;

    private final TournamentRoundProgressService tournamentRoundProgressService;
    private final MeetupRepository meetupRepository;
    private final CourtRepository courtRepository;

    @Transactional(rollbackFor = Exception.class)
    public void selectCourtBooker(String matchId, String userId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        match.selectCourtBooker(userId);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void giveUpCourtBooker(String matchId, String userId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        match.giveUpCourtBooker(userId);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
    }

    /**
     * 提交赛约（订场）：未传 meetupId 时创建草稿约球；传入 meetupId 时更新对应赛事约球。
     */
    @Transactional(rollbackFor = Exception.class)
    public String submitBooking(SubmitBookingCmd cmd, String userId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(cmd.getMatchId());
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        // TEXT/MAP 模式下，通过 courtId 查询球场库数据，球场信息以库数据为准
        CourtData courtData = resolveCourtData(cmd.getCourtSelectMode(), cmd.getCourtId());
        TournamentData tournamentData = tournamentRepository.findByBizId(match.getData().getTournamentId());
        Assert.notNull(tournamentData, BizErrorCode.TOURNAMENT_NOT_FOUND);

        if (StringUtils.isNotBlank(cmd.getMeetupId())) {
            return updateBooking(cmd, userId, match, courtData, tournamentData);
        }

        match.submitBooking(userId);
        Meetup draft = MeetupFactory.createTournamentDraft(cmd, userId, courtData, match.getParticipants(), tournamentData.getTournamentName());
        meetupRepository.save(draft);
        match.getData().setMeetupId(draft.getMeetupId());

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
        return draft.getMeetupId();
    }

    private String updateBooking(SubmitBookingCmd cmd, String userId, TournamentMatch match, CourtData courtData, TournamentData tournamentData) {
        MeetupData meetup = meetupRepository.findByBizId(cmd.getMeetupId());
        Assert.notNull(meetup, BizErrorCode.MEETUP_NOT_FOUND);
        Assert.eq(match.getData().getMeetupId(), cmd.getMeetupId(), BizErrorCode.TOURNAMENT_BOOKING_MEETUP_MISMATCH);
        Assert.eq(meetup.getCreatorId(), userId, BizErrorCode.NOT_CREATOR);

        TournamentMatchStatusEnum status = match.getData().getStatus();
        Assert.isTrue(status == TournamentMatchStatusEnum.BOOKING || status == TournamentMatchStatusEnum.SCHEDULED,
                BizErrorCode.MEETUP_TOURNAMENT_EDIT_FORBIDDEN);

        MeetupDomainConvertMapper.INSTANCE.updateTournamentMeetupData(meetup, cmd, courtData);
        if (StringUtils.isBlank(meetup.getTitle())) {
            meetup.setTitle(tournamentData.getTournamentName());
        }
        meetupRepository.save(meetup);

        // 被打回重订时，更新原约球并重新提交赛约；已提交状态下仅更新约球信息。
        if (status == TournamentMatchStatusEnum.BOOKING) {
            match.submitBooking(userId);
            boolean success = matchRepository.updateWithVersion(match.getData());
            if (!success) {
                throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
            }
            matchRepository.saveParticipants(match.getParticipants());
        }
        return meetup.getBizId();
    }

    /**
     * TEXT/MAP 模式下，通过 courtId 查询球场库数据；FREE 模式或未查到返回 null
     */
    private CourtData resolveCourtData(CourtSelectModeEnum courtSelectMode, String courtId) {
        if ((courtSelectMode == CourtSelectModeEnum.TEXT || courtSelectMode == CourtSelectModeEnum.MAP) && courtId != null && !courtId.trim().isEmpty()) {
            return courtRepository.findByBizId(courtId);
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleScheduleConfirm(String matchId, String userId, boolean confirm, ScheduleRejectReasonEnum rejectReason, RebookReasonEnum rebookReason) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        Tournament tournament = getTournament(match.getData().getTournamentId());
        TournamentEntry userEntry = getUserEntry(match.getData().getTournamentId(), userId);

        int rejectCount = userEntry.getData().getStage() == TournamentEntryStageEnum.QUALIFY ? userEntry.getData().getQualifierRejectCount() : userEntry.getData().getMainDrawRejectCount();

        match.confirmSchedule(userId, confirm, rejectReason, rebookReason, tournament.getData().getQualifierRejectLimit(), tournament.getData().getMainDrawRejectLimit(), userEntry.getData().getStage(), rejectCount);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        if (match.getData().getStatus() == TournamentMatchStatusEnum.REJECTED) {
            if (rejectReason != null) {
                incrementRejectCount(userEntry);
            }
            // 比赛终止：关闭草稿约球 + 双方回 WAITING 匹配池
            settleRejectedMatch(match);
        }

        if (match.getData().getStatus() == TournamentMatchStatusEnum.PENDING_PLAY) {
            // 全员确认赛约，草稿约球转为正常报名状态（DRAFT -> OPEN）
            activateDraftMeetup(match.getData().getMeetupId());
        }
    }

    /**
     * 激活草稿约球：DRAFT -> OPEN
     */
    private void activateDraftMeetup(String meetupId) {
        if (meetupId == null) {
            return;
        }
        MeetupData meetup = meetupRepository.findByBizId(meetupId);
        if (meetup != null && meetup.getStatus() == MeetupStatusEnum.DRAFT) {
            meetup.setStatus(MeetupStatusEnum.OPEN);
            meetupRepository.save(meetup);
        }
    }

    /**
     * 关闭草稿约球：DRAFT -> CLOSED
     */
    private void closeDraftMeetup(String meetupId) {
        if (meetupId == null) {
            return;
        }
        MeetupData meetup = meetupRepository.findByBizId(meetupId);
        if (meetup != null && meetup.getStatus() == MeetupStatusEnum.DRAFT) {
            meetup.setStatus(MeetupStatusEnum.CLOSED);
            meetupRepository.save(meetup);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitResult(String matchId, String userId, Integer winnerEntryNo) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        match.submitResult(userId, winnerEntryNo);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleResultConfirm(String matchId, String userId, boolean confirm, ResultRejectReasonEnum rejectReason) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        Tournament tournament = getTournament(match.getData().getTournamentId());
        TournamentEntry userEntry = getUserEntry(match.getData().getTournamentId(), userId);

        int rejectCount = userEntry.getData().getStage() == TournamentEntryStageEnum.QUALIFY ? userEntry.getData().getQualifierRejectCount() : userEntry.getData().getMainDrawRejectCount();

        match.confirmResult(userId, confirm, rejectReason, tournament.getData().getQualifierRejectLimit(), tournament.getData().getMainDrawRejectLimit(), userEntry.getData().getStage(), rejectCount);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        if (!confirm && rejectReason != null) {
            incrementRejectCount(userEntry);
            // 拒绝结果即拒赛：比赛终止，关闭草稿约球 + 双方回 WAITING 匹配池
            settleRejectedMatch(match);
        }

        if (match.getData().getStatus() == TournamentMatchStatusEnum.COMPLETED) {
            updateEntryStatusOnComplete(match);
            tournamentRoundProgressService.advanceIfReady(match.getData().getTournamentId());
        }
    }

    /**
     * 尚未选出订场人的比赛超时：终止比赛，并将参赛者退回匹配池。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleMatchedTimeout(String matchId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        if (match.getData().getStatus() != TournamentMatchStatusEnum.MATCHED) {
            return;
        }

        match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
        match.getData().setRejectReasonCode("TIMEOUT");
        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        settleRejectedMatch(match);
    }

    /**
     * 赛果确认超时：自动确认未确认的赛果，结算参赛者并推进赛事轮次。
     */
    @Transactional(rollbackFor = Exception.class)
    public void completePendingConfirmTimeout(String matchId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        if (match.getData().getStatus() != TournamentMatchStatusEnum.PENDING_CONFIRM) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        match.getParticipants().forEach(participant -> {
            if (participant.getResultConfirmStatus() == ConfirmStatusEnum.PENDING) {
                participant.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
                participant.setResultConfirmTime(now);
            }
        });
        match.getData().setStatus(TournamentMatchStatusEnum.COMPLETED);
        match.getData().setCompletedTime(now);
        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
        updateEntryStatusOnComplete(match);
        tournamentRoundProgressService.advanceIfReady(match.getData().getTournamentId());
    }

    private Tournament getTournament(String tournamentId) {
        TournamentData tournamentData = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournamentData, BizErrorCode.TOURNAMENT_NOT_FOUND);
        return new Tournament(tournamentData);
    }

    private TournamentEntry getUserEntry(String tournamentId, String userId) {
        TournamentEntryData entryData = entryRepository.findByTournamentAndUser(tournamentId, userId);
        Assert.notNull(entryData, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(entryData);
    }

    /**
     * 退赛联动：关闭该用户在本赛事进行中的比赛（若有）。比赛置 REJECTED、关闭草稿约球，
     * 对手回 WAITING 匹配池（退赛人已置 WITHDRAWN，不会被回退）。退赛不计拒绝次数。
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeActiveMatchOnWithdraw(String tournamentId, String userId) {
        TournamentMatch match = matchRepository.findActiveMatchByTournamentAndUser(tournamentId, userId);
        if (match == null) {
            return;
        }
        match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        settleRejectedMatch(match);
    }

    /**
     * 比赛终止（REJECTED）后的统一落地：关闭草稿约球，并把该场全体参与者的报名状态回退到 WAITING 重新进入匹配池
     * （currentRound 不变；拒赛/退赛人数是否变化交由后续匹配 bye 兜底处理）。拒绝次数由调用方按场景决定是否自增。
     */
    private void settleRejectedMatch(TournamentMatch match) {
        closeDraftMeetup(match.getData().getMeetupId());
        for (MatchParticipantData participant : match.getParticipants()) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), participant.getUserId());
            if (entry.getData().getStatus() == TournamentEntryStatusEnum.IN_MATCH) {
                entry.getData().setStatus(TournamentEntryStatusEnum.WAITING);
                entryRepository.save(entry.getData());
            }
        }
    }

    private void incrementRejectCount(TournamentEntry entry) {
        if (entry.getData().getStage() == TournamentEntryStageEnum.QUALIFY) {
            entry.getData().setQualifierRejectCount(entry.getData().getQualifierRejectCount() + 1);
        } else {
            entry.getData().setMainDrawRejectCount(entry.getData().getMainDrawRejectCount() + 1);
        }
        entryRepository.save(entry.getData());
    }

    private void updateEntryStatusOnComplete(TournamentMatch match) {
        Integer winnerEntryNo = match.getData().getWinnerEntryNo();
        Assert.notNull(winnerEntryNo, BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);
        List<String> winnerUserIds = match.getParticipants().stream()
                .filter(p -> winnerEntryNo.equals(p.getEntryNo()))
                .map(MatchParticipantData::getUserId)
                .collect(Collectors.toList());
        List<String> loserUserIds = match.getParticipants().stream()
                .filter(p -> !winnerEntryNo.equals(p.getEntryNo()))
                .map(MatchParticipantData::getUserId)
                .collect(Collectors.toList());

        for (String userId : winnerUserIds) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), userId);
            entry.advanceAfterWin(match.getData().getRound());
            entryRepository.save(entry.getData());
        }

        for (String userId : loserUserIds) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), userId);
            entry.getData().setStatus(TournamentEntryStatusEnum.ELIMINATED);
            entryRepository.save(entry.getData());
        }
    }
}
