package com.rally.domain.tennis.gateway;

import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.PlayerData;
import com.rally.domain.tennis.model.PlayerSeedData;
import com.rally.domain.tennis.model.SetScoreData;

import java.util.List;

/**
 * 比赛查询 Gateway 接口
 */
public interface MatchQueryGateway {

    /**
     * 根据 tournamentId 列表查询比赛
     * @param tournamentIds tournamentId 列表
     * @return 比赛列表
     */
    List<MatchData> listByTournamentIds(List<String> tournamentIds);

    /**
     * 根据 tennis_match.id 列表查询盘分
     * @param tennisMatchIds tennis_match.id 列表
     * @return 盘分列表
     */
    List<SetScoreData> listSetScoresByTennisMatchIds(List<Long> tennisMatchIds);

    /**
     * 根据球员ID列表查询球员信息
     * @param playerIds 球员ID列表
     * @return 球员列表
     */
    List<PlayerData> listPlayersByPlayerIds(List<String> playerIds);

    /**
     * 根据赛事ID列表查询球员种子信息
     * @param tournamentIds 赛事ID列表
     * @return 种子信息列表
     */
    List<PlayerSeedData> listSeedsByTournamentIds(List<String> tournamentIds);
}
