package com.rally.domain.tour.repository;

import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.PlayerDetailData;
import com.rally.domain.tour.model.PlayerSeedData;
import com.rally.domain.tour.model.SetScoreData;
import com.rally.domain.tour.model.TourDrawData;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 比赛查询 Repository 接口
 */
public interface MatchQueryRepository {

    /**
     * 根据 tournamentId 列表查询已结束比赛（status = FINISHED）
     */
    List<MatchData> listFinishedByTournamentIds(List<String> tournamentIds);

    /**
     * 根据 tournamentId 列表和日期集合查询已结束比赛
     */
    List<MatchData> listFinishedByTournamentIdsAndDates(List<String> tournamentIds, Set<LocalDate> dates);

    /**
     * 根据 tournamentId 列表查询未结束比赛（status != FINISHED 且有比赛日期）
     */
    List<MatchData> listUnfinishedByTournamentIds(List<String> tournamentIds);

    /**
     * 根据 tour_match.id 列表查询盘分
     * @param tourMatchIds tour_match.id 列表
     * @return 盘分列表
     */
    List<SetScoreData> listSetScoresByTourMatchIds(List<Long> tourMatchIds);

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

    /**
     * 查询签表信息
     */
    TourDrawData getDrawByTournamentIdAndType(String tournamentId, Integer year, String drawType);

    /**
     * 查询某签表下某球员参与的所有比赛（含未来场次）
     */
    List<MatchData> listByDrawIdAndPlayerId(Long drawId, String playerId);

    /**
     * 查询某签表下所有比赛（用于前方对手推算）
     */
    List<MatchData> listByDrawId(Long drawId);

    /**
     * 查询球员详细信息（含排名、积分、出生日期）
     */
    PlayerDetailData getPlayerById(String playerId);

    /**
     * 查询球员在某签表中的种子信息
     */
    PlayerSeedData getSeedByDrawIdAndPlayerId(Long drawId, String playerId);

    /**
     * 根据赛事ID列表和日期查询比赛
     */
    List<MatchData> findByTournamentIdsAndDate(List<String> tournamentIds, LocalDate date);
}
