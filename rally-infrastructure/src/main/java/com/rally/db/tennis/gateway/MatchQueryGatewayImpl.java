package com.rally.db.tennis.gateway;

import com.rally.db.tennis.entity.TennisDrawPO;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.service.TennisDrawService;
import com.rally.db.tennis.service.TennisMatchService;
import com.rally.db.tennis.service.TennisPlayerService;
import com.rally.db.tennis.service.TennisSetScoreService;
import com.rally.db.tennis.service.TennisTournamentEntryService;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.PlayerData;
import com.rally.domain.tennis.model.PlayerDetailData;
import com.rally.domain.tennis.model.PlayerSeedData;
import com.rally.domain.tennis.model.SetScoreData;
import com.rally.domain.tennis.model.TennisDrawData;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 比赛查询 Gateway 实现
 */
@Component
@RequiredArgsConstructor
public class MatchQueryGatewayImpl implements MatchQueryGateway {

    private final TennisMatchService matchService;
    private final TennisPlayerService playerService;
    private final TennisSetScoreService setScoreService;
    private final TennisTournamentEntryService tournamentEntryService;
    private final TennisDrawService drawService;

    @Override
    public List<MatchData> listByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TennisMatchPO> list = matchService.lambdaQuery()
                .and(w -> w.isNotNull(TennisMatchPO::getMatchDate)
                        .or()
                        .eq(TennisMatchPO::getStatus, "FINISHED"))
                .in(TennisMatchPO::getTournamentId, tournamentIds)
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<SetScoreData> listSetScoresByTennisMatchIds(List<Long> tennisMatchIds) {
        if (CollectionUtils.isEmpty(tennisMatchIds)) {
            return List.of();
        }
        List<TennisSetScorePO> list = setScoreService.lambdaQuery()
                .in(TennisSetScorePO::getTennisMatchId, tennisMatchIds)
                .orderByAsc(TennisSetScorePO::getSetNumber)
                .list();
        return list.stream().map(this::toSetScoreData).toList();
    }

    @Override
    public List<PlayerData> listPlayersByPlayerIds(List<String> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return List.of();
        }
        List<TennisPlayerPO> list = playerService.lambdaQuery()
                .in(TennisPlayerPO::getPlayerId, playerIds)
                .list();
        return list.stream().map(this::toPlayerData).toList();
    }

    @Override
    public List<PlayerSeedData> listSeedsByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TennisDrawPO> draws = drawService.lambdaQuery()
                .in(TennisDrawPO::getTournamentId, tournamentIds)
                .list();
        if (CollectionUtils.isEmpty(draws)) {
            return List.of();
        }
        Map<Long, String> drawIdToTournamentId = draws.stream()
                .collect(java.util.stream.Collectors.toMap(TennisDrawPO::getId, TennisDrawPO::getTournamentId));

        List<TennisTournamentEntryPO> list = tournamentEntryService.lambdaQuery()
                .in(TennisTournamentEntryPO::getDrawId, drawIdToTournamentId.keySet())
                .isNotNull(TennisTournamentEntryPO::getSeed)
                .ne(TennisTournamentEntryPO::getSeed, 0)
                .list();
        return list.stream()
                .map(po -> toPlayerSeedData(po, drawIdToTournamentId.get(po.getDrawId())))
                .toList();
    }

    private MatchData toMatchData(TennisMatchPO po) {
        MatchData data = new MatchData();
        data.setMatchId(po.getMatchId());
        data.setTennisMatchId(po.getId());
        data.setDrawId(po.getDrawId());
        data.setMatchIndex(po.getMatchIndex());
        data.setRoundNumber(po.getRoundNumber());
        data.setTournamentId(po.getTournamentId());
        data.setPlayer1Id(po.getPlayer1Id());
        data.setPlayer2Id(po.getPlayer2Id());
        data.setWinnerId(po.getWinnerId());
        data.setRoundName(po.getRoundName());
        data.setCourt(po.getCourt());
        data.setCourtSeq(po.getCourtSeq());
        data.setStatus(po.getStatus());
        data.setDurationMinutes(po.getDurationMinutes());
        data.setScheduledAtText(po.getScheduledAtText());
        data.setMatchDate(po.getMatchDate());
        data.setScheduledAt(po.getScheduledAt());
        data.setStartedAt(po.getStartedAt());
        return data;
    }

    private SetScoreData toSetScoreData(TennisSetScorePO po) {
        SetScoreData data = new SetScoreData();
        data.setTennisMatchId(po.getTennisMatchId());
        data.setSetNumber(po.getSetNumber());
        data.setP1Games(po.getP1Games());
        data.setP2Games(po.getP2Games());
        data.setP1Tiebreak(po.getP1Tiebreak());
        data.setP2Tiebreak(po.getP2Tiebreak());
        return data;
    }

    private PlayerData toPlayerData(TennisPlayerPO po) {
        PlayerData data = new PlayerData();
        data.setPlayerId(po.getPlayerId());
        data.setFirstName(po.getFirstName());
        data.setLastName(po.getLastName());
        data.setNationality(po.getNationality());
        return data;
    }

    private PlayerSeedData toPlayerSeedData(TennisTournamentEntryPO po, String tournamentId) {
        PlayerSeedData data = new PlayerSeedData();
        data.setTournamentId(tournamentId);
        data.setPlayerId(po.getPlayerId());
        data.setSeed(po.getSeed() != null ? po.getSeed().intValue() : null);
        return data;
    }

    @Override
    public TennisDrawData getDrawByTournamentIdAndType(String tournamentId, Integer year, String drawType) {
        TennisDrawPO po = drawService.lambdaQuery()
                .eq(TennisDrawPO::getTournamentId, tournamentId)
                .eq(TennisDrawPO::getYear, year)
                .eq(TennisDrawPO::getDrawType, drawType)
                .one();
        if (po == null) {
            return null;
        }
        TennisDrawData data = new TennisDrawData();
        data.setId(po.getId());
        data.setTournamentId(po.getTournamentId());
        data.setYear(po.getYear());
        data.setDrawType(po.getDrawType());
        data.setSize(po.getSize());
        data.setTotalRounds(po.getTotalRounds());
        return data;
    }

    @Override
    public List<MatchData> listByDrawIdAndPlayerId(Long drawId, String playerId) {
        List<TennisMatchPO> list = matchService.lambdaQuery()
                .eq(TennisMatchPO::getDrawId, drawId)
                .and(w -> w.eq(TennisMatchPO::getPlayer1Id, playerId)
                        .or()
                        .eq(TennisMatchPO::getPlayer2Id, playerId))
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<MatchData> listByDrawId(Long drawId) {
        List<TennisMatchPO> list = matchService.lambdaQuery()
                .eq(TennisMatchPO::getDrawId, drawId)
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public PlayerDetailData getPlayerById(String playerId) {
        TennisPlayerPO po = playerService.lambdaQuery()
                .eq(TennisPlayerPO::getPlayerId, playerId)
                .one();
        if (po == null) {
            return null;
        }
        PlayerDetailData data = new PlayerDetailData();
        data.setPlayerId(po.getPlayerId());
        data.setFirstName(po.getFirstName());
        data.setLastName(po.getLastName());
        data.setNationality(po.getNationality());
        data.setRank(po.getRank());
        data.setPoints(po.getPoints());
        data.setBirthDate(po.getBirthDate());
        return data;
    }

    @Override
    public PlayerSeedData getSeedByDrawIdAndPlayerId(Long drawId, String playerId) {
        TennisTournamentEntryPO po = tournamentEntryService.lambdaQuery()
                .eq(TennisTournamentEntryPO::getDrawId, drawId)
                .eq(TennisTournamentEntryPO::getPlayerId, playerId)
                .one();
        if (po == null) {
            return null;
        }
        PlayerSeedData data = new PlayerSeedData();
        data.setPlayerId(po.getPlayerId());
        data.setSeed(po.getSeed() != null ? po.getSeed().intValue() : null);
        return data;
    }

    @Override
    public List<MatchData> findByTournamentIdsAndDate(List<String> tournamentIds, LocalDate date) {
        List<TennisMatchPO> list = matchService.findByTournamentIdsAndDate(tournamentIds, date);
        return list.stream().map(this::toMatchData).toList();
    }
}
