package com.rally.tournament.shared;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentMatchRejectionSupport {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final MeetupRepository meetupRepository;

    public TournamentMatch requireMatch(String matchId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return match;
    }

    public TournamentData requireTournament(String tournamentId) {
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);
        return tournament;
    }

    public TournamentEntry requireEntry(String tournamentId, String userId) {
        TournamentEntryData entry = entryRepository.findByTournamentAndUser(tournamentId, userId);
        Assert.notNull(entry, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(entry);
    }

    public int rejectCount(TournamentEntry entry) {
        return entry.getData().getStage() == TournamentEntryStageEnum.QUALIFY
                ? entry.getData().getQualifierRejectCount()
                : entry.getData().getMainDrawRejectCount();
    }

    public void persistMatch(TournamentMatch match, boolean saveParticipants) {
        if (!matchRepository.updateWithVersion(match.getData())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        if (saveParticipants) {
            matchRepository.saveParticipants(match.getParticipants());
        }
    }

    public void persistRejectedMatch(TournamentMatch match) {
        persistMatch(match, true);
        settleRejectedMatch(match);
    }

    public void settleRejectedMatch(TournamentMatch match) {
        closeDraftMeetup(match.getData().getMeetupId());
        for (MatchParticipantData participant : match.getParticipants()) {
            TournamentEntry entry = requireEntry(
                    match.getData().getTournamentId(), participant.getUserId());
            if (entry.getData().getStatus() == TournamentEntryStatusEnum.IN_MATCH) {
                entry.getData().setStatus(TournamentEntryStatusEnum.WAITING);
                entryRepository.save(entry.getData());
            }
        }
    }

    public void incrementRejectCount(TournamentEntry entry) {
        if (entry.getData().getStage() == TournamentEntryStageEnum.QUALIFY) {
            entry.getData().setQualifierRejectCount(
                    entry.getData().getQualifierRejectCount() + 1);
        } else {
            entry.getData().setMainDrawRejectCount(
                    entry.getData().getMainDrawRejectCount() + 1);
        }
        entryRepository.save(entry.getData());
    }

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
}
