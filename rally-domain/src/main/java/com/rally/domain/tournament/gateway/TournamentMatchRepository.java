package com.rally.domain.tournament.gateway;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛表读写网关接口
 */
public interface TournamentMatchRepository {

    /**
     * 保存比赛（新增或更新）
     */
    void save(TournamentMatchData data);

    /**
     * 带乐观锁检查的更新，返回是否成功
     */
    boolean updateWithVersion(TournamentMatchData data);

    /**
     * 保存参与者（新增或更新）
     */
    void saveParticipants(List<MatchParticipantData> participants);

    /**
     * 删除尚未提交订场信息的比赛及其参与者。仅 MATCHED、BOOKING 状态可删除，返回 false 表示比赛已被并发更新。
     */
    boolean deleteUnsubmittedWithParticipants(String matchId);

    /**
     * 根据 bizId 查询
     */
    TournamentMatchData findByBizId(String bizId);

    /**
     * 根据关联的约球 meetupId 查询比赛，无则返回 null
     */
    TournamentMatchData findByMeetupId(String meetupId);

    /**
     * 加载单个match+participants
     */
    TournamentMatch findByBizIdWithParticipants(String matchId);

    /**
     * 在当前事务中锁定比赛根后加载 match+participants。
     */
    TournamentMatch findByBizIdWithParticipantsForUpdate(String matchId);

    /**
     * 查询某场比赛的所有参与者
     */
    List<MatchParticipantData> findParticipantsByMatchId(String matchId);

    /**
     * 分配赛事内递增的 matchNo（取当前赛事已有最大 matchNo + 1）
     */
    int nextMatchNo(String tournamentId);

    /**
     * 查询某赛事下所有已终止（REJECTED）比赛的参与者，用于反查互相拒绝历史
     */
    List<MatchParticipantData> findRejectedParticipantsByTournament(String tournamentId);

    /**
     * 超时扫描查询
     */
    List<TournamentMatch> findTimeoutMatches(TournamentMatchStatusEnum status, LocalDateTime timeoutBefore);

    /**
     * 查询某赛事下所有比赛（签表 + 轮次进度统计用）
     */
    List<TournamentMatchData> findByTournamentId(String tournamentId);

    /**
     * 批量查询多场比赛的参与者
     */
    List<MatchParticipantData> findParticipantsByMatchIds(List<String> matchIds);

    /**
     * 查询某用户在某赛事下当前进行中（非 COMPLETED/REJECTED）的比赛，无则返回 null
     */
    TournamentMatch findActiveMatchByTournamentAndUser(String tournamentId, String userId);

    /** 指定用户是否参与该赛事任一进行中比赛。 */
    boolean existsActiveMatchByTournamentAndUser(String tournamentId, String userId);

    /**
     * 统计某赛事下已生成的比赛总场数
     */
    int countByTournamentId(String tournamentId);

    /**
     * 统计某赛事某轮次下已完成（COMPLETED）的比赛场数
     */
    int countCompletedByTournamentAndRound(String tournamentId, TournamentRoundEnum round);
}
