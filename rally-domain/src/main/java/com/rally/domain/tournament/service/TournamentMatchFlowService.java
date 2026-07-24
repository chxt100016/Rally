package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentMatchFlowService {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
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
     * 提交赛约（订场）：比赛进入 SCHEDULED，并按约球全量数据创建草稿约球（DRAFT），返回草稿 meetupId。
     * 场地/时间等数据只落在约球上，比赛仅通过 meetupId 关联。后续修改由订场人跳转约球活动页编辑。
     */
    @Transactional(rollbackFor = Exception.class)
    public String submitBooking(SubmitBookingCmd cmd, String userId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(cmd.getMatchId());
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        match.submitBooking(userId);

        // TEXT/MAP 模式下，通过 courtId 查询球场库数据，球场信息以库数据为准
        CourtData courtData = resolveCourtData(cmd.getCourtSelectMode(), cmd.getCourtId());
        Meetup draft = MeetupFactory.createTournamentDraft(cmd, userId, courtData, match.getParticipants());
        meetupRepository.save(draft);
        match.getData().setMeetupId(draft.getMeetupId());

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
        return draft.getMeetupId();
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
    public void handleScheduleConfirm(String matchId, String userId, boolean confirm, ScheduleRejectReasonEnum rejectReason, String rejectReasonText, RebookReasonEnum rebookReason, String rebookReasonText) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        Tournament tournament = getTournament(match.getData().getTournamentId());
        TournamentEntry userEntry = getUserEntry(match.getData().getTournamentId(), userId);

        int rejectCount = userEntry.getData().getStage() == TournamentEntryStageEnum.QUALIFY ? userEntry.getData().getQualifierRejectCount() : userEntry.getData().getMainDrawRejectCount();

        match.confirmSchedule(userId, confirm, rejectReason, rejectReasonText, rebookReason, rebookReasonText, tournament.getData().getQualifierRejectLimit(), tournament.getData().getMainDrawRejectLimit(), userEntry.getData().getStage(), rejectCount);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        if (match.getData().getStatus() == TournamentMatchStatusEnum.REJECTED) {
            if (rejectReason != null) {
                incrementRejectCount(userEntry);
            }
            // 比赛终止，关闭草稿约球
            closeDraftMeetup(match.getData().getMeetupId());
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
    public void submitResult(String matchId, String userId, List<String> winnerUserIds) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        match.submitResult(userId, winnerUserIds);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleResultConfirm(String matchId, String userId, boolean confirm, ResultRejectReasonEnum rejectReason, String rejectReasonText) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        Tournament tournament = getTournament(match.getData().getTournamentId());
        TournamentEntry userEntry = getUserEntry(match.getData().getTournamentId(), userId);

        int rejectCount = userEntry.getData().getStage() == TournamentEntryStageEnum.QUALIFY ? userEntry.getData().getQualifierRejectCount() : userEntry.getData().getMainDrawRejectCount();

        match.confirmResult(userId, confirm, rejectReason, rejectReasonText, tournament.getData().getQualifierRejectLimit(), tournament.getData().getMainDrawRejectLimit(), userEntry.getData().getStage(), rejectCount);

        boolean success = matchRepository.updateWithVersion(match.getData());
        if (!success) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        if (!confirm && rejectReason != null) {
            incrementRejectCount(userEntry);
        }

        if (match.getData().getStatus() == TournamentMatchStatusEnum.COMPLETED) {
            updateEntryStatusOnComplete(match);
        }
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

    private void incrementRejectCount(TournamentEntry entry) {
        if (entry.getData().getStage() == TournamentEntryStageEnum.QUALIFY) {
            entry.getData().setQualifierRejectCount(entry.getData().getQualifierRejectCount() + 1);
        } else {
            entry.getData().setMainDrawRejectCount(entry.getData().getMainDrawRejectCount() + 1);
        }
        entryRepository.save(entry.getData());
    }

    private void updateEntryStatusOnComplete(TournamentMatch match) {
        List<String> winnerUserIds = match.getParticipants().stream().filter(p -> p.getIsWinner() != null && p.getIsWinner()).map(MatchParticipantData::getUserId).collect(Collectors.toList());
        List<String> loserUserIds = match.getParticipants().stream().filter(p -> p.getIsWinner() != null && !p.getIsWinner()).map(MatchParticipantData::getUserId).collect(Collectors.toList());

        for (String userId : winnerUserIds) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), userId);
            if (entry.getData().getStage() == TournamentEntryStageEnum.QUALIFY) {
                entry.getData().setStatus(TournamentEntryStatusEnum.PAYING);
            } else {
                entry.getData().setStatus(TournamentEntryStatusEnum.WAITING);
            }
            entryRepository.save(entry.getData());
        }

        for (String userId : loserUserIds) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), userId);
            entry.getData().setStatus(TournamentEntryStatusEnum.ELIMINATED);
            entryRepository.save(entry.getData());
        }
    }
}
