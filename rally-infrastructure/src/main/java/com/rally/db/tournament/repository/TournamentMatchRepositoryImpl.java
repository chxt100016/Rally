package com.rally.db.tournament.repository;

import com.rally.db.tournament.convert.MatchParticipantConvertMapper;
import com.rally.db.tournament.convert.TournamentMatchConvertMapper;
import com.rally.db.tournament.entity.MatchParticipantPO;
import com.rally.db.tournament.entity.TournamentMatchPO;
import com.rally.db.tournament.service.MatchParticipantMybatisService;
import com.rally.db.tournament.service.TournamentMatchMybatisService;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 比赛表 Repository 实现
 */
@Component
@RequiredArgsConstructor
public class TournamentMatchRepositoryImpl implements TournamentMatchRepository {

    private final TournamentMatchMybatisService tournamentMatchService;
    private final MatchParticipantMybatisService matchParticipantService;
    private static final TournamentMatchConvertMapper MATCH_MAPPER = TournamentMatchConvertMapper.INSTANCE;
    private static final MatchParticipantConvertMapper PARTICIPANT_MAPPER = MatchParticipantConvertMapper.INSTANCE;

    @Override
    public void save(TournamentMatchData data) {
        TournamentMatchPO po = MATCH_MAPPER.toTournamentMatchPO(data);
        boolean updated = po.getBizId() != null && tournamentMatchService.lambdaUpdate().eq(TournamentMatchPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            tournamentMatchService.save(po);
        }
    }

    @Override
    public boolean updateWithVersion(TournamentMatchData data) {
        TournamentMatchPO po = MATCH_MAPPER.toTournamentMatchPO(data);
        return tournamentMatchService.lambdaUpdate().eq(TournamentMatchPO::getBizId, po.getBizId()).eq(TournamentMatchPO::getVersion, data.getVersion()).update(po);
    }

    @Override
    public void saveParticipants(List<MatchParticipantData> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        List<MatchParticipantPO> pos = participants.stream().map(PARTICIPANT_MAPPER::toMatchParticipantPO).collect(Collectors.toList());
        for (MatchParticipantPO po : pos) {
            boolean updated = po.getBizId() != null && matchParticipantService.lambdaUpdate().eq(MatchParticipantPO::getBizId, po.getBizId()).update(po);
            if (!updated) {
                matchParticipantService.save(po);
            }
        }
    }

    @Override
    public TournamentMatchData findByBizId(String bizId) {
        TournamentMatchPO po = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getBizId, bizId).one();
        return MATCH_MAPPER.toTournamentMatchData(po);
    }

    @Override
    public TournamentMatch findByBizIdWithParticipants(String matchId) {
        TournamentMatchData matchData = findByBizId(matchId);
        if (matchData == null) {
            return null;
        }
        List<MatchParticipantData> participants = findParticipantsByMatchId(matchId);
        return new TournamentMatch(matchData, participants);
    }

    @Override
    public List<MatchParticipantData> findParticipantsByMatchId(String matchId) {
        List<MatchParticipantPO> pos = matchParticipantService.lambdaQuery().eq(MatchParticipantPO::getMatchId, matchId).list();
        return pos.stream().map(PARTICIPANT_MAPPER::toMatchParticipantData).collect(Collectors.toList());
    }

    @Override
    public int nextMatchNo(String tournamentId) {
        TournamentMatchPO maxMatch = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getTournamentId, tournamentId).orderByDesc(TournamentMatchPO::getMatchNo).last("LIMIT 1").one();
        return maxMatch == null ? 1 : maxMatch.getMatchNo() + 1;
    }

    @Override
    public List<MatchParticipantData> findRejectedParticipantsByTournament(String tournamentId) {
        List<TournamentMatchPO> rejectedMatches = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getTournamentId, tournamentId).eq(TournamentMatchPO::getStatus, TournamentMatchStatusEnum.REJECTED.name()).list();
        if (rejectedMatches.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> matchIds = rejectedMatches.stream().map(TournamentMatchPO::getBizId).collect(Collectors.toList());
        List<MatchParticipantPO> pos = matchParticipantService.lambdaQuery().in(MatchParticipantPO::getMatchId, matchIds).list();
        return pos.stream().map(PARTICIPANT_MAPPER::toMatchParticipantData).collect(Collectors.toList());
    }

    @Override
    public List<TournamentMatch> findTimeoutMatches(TournamentMatchStatusEnum status, LocalDateTime timeoutBefore) {
        List<TournamentMatchPO> matchPOs;
        switch (status) {
            case MATCHED:
                matchPOs = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getStatus, status.name()).le(TournamentMatchPO::getMatchedTime, timeoutBefore).list();
                break;
            case BOOKING:
                matchPOs = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getStatus, status.name()).and(wrapper -> wrapper.le(TournamentMatchPO::getLastRebookTime, timeoutBefore).or().le(TournamentMatchPO::getCourtBookerSelectedTime, timeoutBefore)).list();
                break;
            case SCHEDULED:
                matchPOs = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getStatus, status.name()).le(TournamentMatchPO::getScheduleSubmittedTime, timeoutBefore).list();
                break;
            case PENDING_CONFIRM:
                matchPOs = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getStatus, status.name()).le(TournamentMatchPO::getSubmittedTime, timeoutBefore).list();
                break;
            default:
                matchPOs = Collections.emptyList();
        }
        return matchPOs.stream().map(po -> {
            TournamentMatchData data = MATCH_MAPPER.toTournamentMatchData(po);
            List<MatchParticipantData> participants = findParticipantsByMatchId(po.getBizId());
            return new TournamentMatch(data, participants);
        }).collect(Collectors.toList());
    }

    @Override
    public int countByTournamentId(String tournamentId) {
        return Math.toIntExact(tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getTournamentId, tournamentId).count());
    }

    @Override
    public List<TournamentMatchData> findByTournamentId(String tournamentId) {
        List<TournamentMatchPO> pos = tournamentMatchService.lambdaQuery().eq(TournamentMatchPO::getTournamentId, tournamentId).list();
        return pos.stream().map(MATCH_MAPPER::toTournamentMatchData).collect(Collectors.toList());
    }

    @Override
    public List<MatchParticipantData> findParticipantsByMatchIds(List<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<MatchParticipantPO> pos = matchParticipantService.lambdaQuery().in(MatchParticipantPO::getMatchId, matchIds).list();
        return pos.stream().map(PARTICIPANT_MAPPER::toMatchParticipantData).collect(Collectors.toList());
    }

    @Override
    public TournamentMatch findActiveMatchByTournamentAndUser(String tournamentId, String userId) {
        List<MatchParticipantPO> myParticipations = matchParticipantService.lambdaQuery().eq(MatchParticipantPO::getTournamentId, tournamentId).eq(MatchParticipantPO::getUserId, userId).list();
        if (myParticipations.isEmpty()) {
            return null;
        }
        List<String> matchIds = myParticipations.stream().map(MatchParticipantPO::getMatchId).collect(Collectors.toList());
        TournamentMatchPO activePo = tournamentMatchService.lambdaQuery().in(TournamentMatchPO::getBizId, matchIds)
                .notIn(TournamentMatchPO::getStatus, TournamentMatchStatusEnum.COMPLETED.name(), TournamentMatchStatusEnum.REJECTED.name())
                .last("LIMIT 1").one();
        if (activePo == null) {
            return null;
        }
        TournamentMatchData data = MATCH_MAPPER.toTournamentMatchData(activePo);
        List<MatchParticipantData> participants = findParticipantsByMatchId(activePo.getBizId());
        return new TournamentMatch(data, participants);
    }

}

