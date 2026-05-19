package com.rally.db.tennis.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.tennis.entity.TennisDrawPO;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.mapper.TennisDrawMapper;
import com.rally.db.tennis.mapper.TennisMatchMapper;
import com.rally.db.tennis.mapper.TennisPlayerMapper;
import com.rally.db.tennis.mapper.TennisSetScoreMapper;
import com.rally.db.tennis.mapper.TennisTournamentEntryMapper;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.PlayerData;
import com.rally.domain.tennis.model.PlayerSeedData;
import com.rally.domain.tennis.model.SetScoreData;
import lombok.RequiredArgsConstructor;
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

    private final TennisMatchMapper matchMapper;
    private final TennisPlayerMapper playerMapper;
    private final TennisSetScoreMapper setScoreMapper;
    private final TennisTournamentEntryMapper tournamentEntryMapper;
    private final TennisDrawMapper drawMapper;

    @Override
    public List<MatchData> listByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TennisMatchPO> list = matchMapper.selectList(
                new LambdaQueryWrapper<TennisMatchPO>()
                        .and(w -> w.isNotNull(TennisMatchPO::getMatchDate)
                                .or()
                                .eq(TennisMatchPO::getStatus, "FINISHED"))
                        .in(TennisMatchPO::getTournamentId, tournamentIds)
        );
        return list.stream().map(this::toMatchData).toList();
    }

    @Override
    public List<SetScoreData> listSetScoresByTennisMatchIds(List<Long> tennisMatchIds) {
        if (CollectionUtils.isEmpty(tennisMatchIds)) {
            return List.of();
        }
        List<TennisSetScorePO> list = setScoreMapper.selectList(
                new LambdaQueryWrapper<TennisSetScorePO>()
                        .in(TennisSetScorePO::getTennisMatchId, tennisMatchIds)
                        .orderByAsc(TennisSetScorePO::getSetNumber)
        );
        return list.stream().map(this::toSetScoreData).toList();
    }

    @Override
    public List<PlayerData> listPlayersByPlayerIds(List<String> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return List.of();
        }
        List<TennisPlayerPO> list = playerMapper.selectList(
                new LambdaQueryWrapper<TennisPlayerPO>()
                        .in(TennisPlayerPO::getPlayerId, playerIds)
        );
        return list.stream().map(this::toPlayerData).toList();
    }

    @Override
    public List<PlayerSeedData> listSeedsByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        List<TennisDrawPO> draws = drawMapper.selectList(
                new LambdaQueryWrapper<TennisDrawPO>()
                        .in(TennisDrawPO::getTournamentId, tournamentIds)
        );
        if (CollectionUtils.isEmpty(draws)) {
            return List.of();
        }
        Map<Long, String> drawIdToTournamentId = draws.stream()
                .collect(java.util.stream.Collectors.toMap(TennisDrawPO::getId, TennisDrawPO::getTournamentId));

        List<TennisTournamentEntryPO> list = tournamentEntryMapper.selectList(
                new LambdaQueryWrapper<TennisTournamentEntryPO>()
                        .in(TennisTournamentEntryPO::getDrawId, drawIdToTournamentId.keySet())
                        .isNotNull(TennisTournamentEntryPO::getSeed)
        );
        return list.stream()
                .map(po -> toPlayerSeedData(po, drawIdToTournamentId.get(po.getDrawId())))
                .toList();
    }

    private MatchData toMatchData(TennisMatchPO po) {
        MatchData data = new MatchData();
        data.setMatchId(po.getMatchId());
        data.setTennisMatchId(po.getId());
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
}
