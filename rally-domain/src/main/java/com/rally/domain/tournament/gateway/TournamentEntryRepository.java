package com.rally.domain.tournament.gateway;

import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.model.TournamentEntryData;

import java.util.List;

/**
 * 报名表读写网关接口
 */
public interface TournamentEntryRepository {

    /**
     * 保存报名（新增或更新）
     */
    void save(TournamentEntryData data);

    /**
     * 根据 bizId 查询
     */
    TournamentEntryData findByBizId(String bizId);

    /**
     * 根据赛事+用户查询（唯一约束）
     */
    TournamentEntryData findByTournamentAndUser(String tournamentId, String userId);

    /**
     * 查询某赛事下所有报名记录
     */
    List<TournamentEntryData> findByTournamentId(String tournamentId);

    /**
     * 查询某赛事某阶段某轮次下排队等待匹配（WAITING）的候选人
     */
    List<TournamentEntryData> findWaitingByTournamentAndStage(String tournamentId, TournamentEntryStageEnum stage, TournamentRoundEnum round);

    /**
     * 查询某赛事某阶段下所有排队候选人（WAITING）实际存在的轮次（去重），用于正赛逐轮匹配时确定需要跑哪些轮次
     */
    List<TournamentRoundEnum> findDistinctWaitingRounds(String tournamentId, TournamentEntryStageEnum stage);
}
