package com.rally.domain.tournament.gateway;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.model.TournamentAdminListCmd;
import com.rally.domain.tournament.model.TournamentData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事主表读写网关接口
 */
public interface TournamentRepository {

    /**
     * 保存赛事（新增或更新）
     */
    void save(TournamentData data);

    /**
     * 根据 bizId 查询
     */
    TournamentData findByBizId(String bizId);

    /**
     * 后台分页列表（城市/状态/NTRP过滤）
     */
    PageDTO<TournamentData> pageList(TournamentAdminListCmd cmd);

    /**
     * 查询已激活且资格赛开始时间已到的赛事，供批量匹配 Job 扫描
     */
    List<TournamentData> findActiveWithQualifierStarted(LocalDateTime now);

    /**
     * 原子扣位：where current_filled_slots &lt; total_slots 时 +1，防超卖。返回是否成功
     */
    boolean incrementFilledSlots(String tournamentId);
}
