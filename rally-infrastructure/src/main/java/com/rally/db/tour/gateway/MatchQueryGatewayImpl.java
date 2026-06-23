package com.rally.db.tour.gateway;

import com.rally.db.tour.entity.TourDrawPO;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.entity.TourPlayerPO;
import com.rally.db.tour.entity.TourSetScorePO;
import com.rally.db.tour.entity.TourTournamentEntryPO;
import com.rally.db.tour.service.TourDrawService;
import com.rally.db.tour.service.TourMatchService;
import com.rally.db.tour.service.TourPlayerService;
import com.rally.db.tour.service.TourSetScoreService;
import com.rally.db.tour.service.TourTournamentEntryService;
import com.rally.domain.tour.gateway.MatchQueryGateway;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.PlayerDetailData;
import com.rally.domain.tour.model.PlayerSeedData;
import com.rally.domain.tour.model.SetScoreData;
import com.rally.domain.tour.model.TourDrawData;
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

    private final TourMatchService matchService;
    private final TourPlayerService playerService;
    private final TourSetScoreService setScoreService;
    private final TourTournamentEntryService tournamentEntryService;
    private final TourDrawService drawService;

    @Override
    public List<MatchData> listFinishedByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TourMatchPO> list = matchService.lambdaQuery()
                .eq(TourMatchPO::getStatus, "FINISHED")
                .in(TourMatchPO::getTournamentId, tournamentIds)
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<MatchData> listUnfinishedByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TourMatchPO> list = matchService.lambdaQuery()
                .isNotNull(TourMatchPO::getMatchDate)
                .and(w -> w.ne(TourMatchPO::getStatus, "FINISHED").or().isNull(TourMatchPO::getStatus))
                .in(TourMatchPO::getTournamentId, tournamentIds)
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<SetScoreData> listSetScoresByTourMatchIds(List<Long> tourMatchIds) {
        if (CollectionUtils.isEmpty(tourMatchIds)) {
            return List.of();
        }
        List<TourSetScorePO> list = setScoreService.lambdaQuery()
                .in(TourSetScorePO::getTourMatchId, tourMatchIds)
                .orderByAsc(TourSetScorePO::getSetNumber)
                .list();
        return list.stream().map(this::toSetScoreData).toList();
    }

    @Override
    public List<PlayerData> listPlayersByPlayerIds(List<String> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return List.of();
        }
        List<TourPlayerPO> list = playerService.lambdaQuery()
                .in(TourPlayerPO::getPlayerId, playerIds)
                .list();
        return list.stream().map(this::toPlayerData).toList();
    }

    @Override
    public List<PlayerSeedData> listSeedsByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TourDrawPO> draws = drawService.lambdaQuery()
                .in(TourDrawPO::getTournamentId, tournamentIds)
                .list();
        if (CollectionUtils.isEmpty(draws)) {
            return List.of();
        }
        Map<Long, String> drawIdToTournamentId = draws.stream()
                .collect(java.util.stream.Collectors.toMap(TourDrawPO::getId, TourDrawPO::getTournamentId));

        List<TourTournamentEntryPO> list = tournamentEntryService.lambdaQuery()
                .in(TourTournamentEntryPO::getDrawId, drawIdToTournamentId.keySet())
                .isNotNull(TourTournamentEntryPO::getSeed)
                .ne(TourTournamentEntryPO::getSeed, 0)
                .list();
        return list.stream()
                .map(po -> toPlayerSeedData(po, drawIdToTournamentId.get(po.getDrawId())))
                .toList();
    }

    private MatchData toMatchData(TourMatchPO po) {
        MatchData data = new MatchData();
        data.setMatchId(po.getMatchId());
        data.setTourMatchId(po.getId());
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

    private SetScoreData toSetScoreData(TourSetScorePO po) {
        SetScoreData data = new SetScoreData();
        data.setTourMatchId(po.getTourMatchId());
        data.setSetNumber(po.getSetNumber());
        data.setP1Games(po.getP1Games());
        data.setP2Games(po.getP2Games());
        data.setP1Tiebreak(po.getP1Tiebreak());
        data.setP2Tiebreak(po.getP2Tiebreak());
        return data;
    }

    private PlayerData toPlayerData(TourPlayerPO po) {
        PlayerData data = new PlayerData();
        data.setPlayerId(po.getPlayerId());
        data.setFirstName(po.getFirstName());
        data.setLastName(po.getLastName());
        data.setNationality(po.getNationality());
        return data;
    }

    private PlayerSeedData toPlayerSeedData(TourTournamentEntryPO po, String tournamentId) {
        PlayerSeedData data = new PlayerSeedData();
        data.setTournamentId(tournamentId);
        data.setPlayerId(po.getPlayerId());
        data.setSeed(po.getSeed() != null ? po.getSeed().intValue() : null);
        return data;
    }

    @Override
    public TourDrawData getDrawByTournamentIdAndType(String tournamentId, Integer year, String drawType) {
        TourDrawPO po = drawService.lambdaQuery()
                .eq(TourDrawPO::getTournamentId, tournamentId)
                .eq(TourDrawPO::getYear, year)
                .eq(TourDrawPO::getDrawType, drawType)
                .one();
        if (po == null) {
            return null;
        }
        TourDrawData data = new TourDrawData();
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
        List<TourMatchPO> list = matchService.lambdaQuery()
                .eq(TourMatchPO::getDrawId, drawId)
                .and(w -> w.eq(TourMatchPO::getPlayer1Id, playerId)
                        .or()
                        .eq(TourMatchPO::getPlayer2Id, playerId))
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<MatchData> listByDrawId(Long drawId) {
        List<TourMatchPO> list = matchService.lambdaQuery()
                .eq(TourMatchPO::getDrawId, drawId)
                .list();
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public PlayerDetailData getPlayerById(String playerId) {
        TourPlayerPO po = playerService.lambdaQuery()
                .eq(TourPlayerPO::getPlayerId, playerId)
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
        TourTournamentEntryPO po = tournamentEntryService.lambdaQuery()
                .eq(TourTournamentEntryPO::getDrawId, drawId)
                .eq(TourTournamentEntryPO::getPlayerId, playerId)
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
        List<TourMatchPO> list = matchService.findByTournamentIdsAndDate(tournamentIds, date);
        return list.stream().map(this::toMatchData).toList();
    }
}
