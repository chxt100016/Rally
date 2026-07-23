package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.convert.TournamentDomainConvertMapper;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 赛事管理领域服务（运营后台：创建/编辑/激活/废弃/列表）
 */
@Service
@RequiredArgsConstructor
public class TournamentAdminService {

    private final TournamentRepository tournamentRepository;

    private final TournamentPolicy tournamentPolicy;

    /**
     * 获取赛事聚合根
     */
    public Tournament get(String tournamentId) {
        TournamentData data = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_NOT_FOUND);
        return new Tournament(data);
    }

    /**
     * 创建赛事草稿
     */
    public Tournament create(TournamentCreateCmd cmd) {
        tournamentPolicy.assertParam(cmd);
        Tournament tournament = TournamentFactory.create(cmd);
        tournamentRepository.save(tournament.getData());
        return tournament;
    }

    /**
     * 编辑草稿：仅 DRAFT 可改，校验后覆盖配置
     */
    public void update(TournamentUpdateCmd cmd) {
        Tournament tournament = get(cmd.getTournamentId());
        tournament.assertCanEdit();
        tournamentPolicy.assertParam(cmd);
        TournamentDomainConvertMapper.INSTANCE.updateTournamentData(tournament.getData(), cmd);
        tournamentRepository.save(tournament.getData());
    }

    /**
     * 激活：校验配置完整性与时间点合法性，DRAFT → ACTIVE
     */
    public void activate(String tournamentId) {
        Tournament tournament = get(tournamentId);
        tournament.activate();
        tournamentRepository.save(tournament.getData());
    }

    /**
     * 废弃：任意非终止态 → ABANDONED
     */
    public void abandon(String tournamentId) {
        Tournament tournament = get(tournamentId);
        tournament.abandon();
        tournamentRepository.save(tournament.getData());
    }

    /**
     * 后台分页列表
     */
    public PageDTO<TournamentData> pageList(TournamentAdminListCmd cmd) {
        return tournamentRepository.pageList(cmd);
    }
}
