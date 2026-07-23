package com.rally.tournament;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.model.*;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.tournament.convert.TournamentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 赛事管理（运营后台）写流程编排：创建/编辑/激活/废弃/列表
 */
@Service
@RequiredArgsConstructor
public class TournamentAdminAppService {

    private final TournamentAdminService tournamentAdminService;

    /**
     * 创建赛事草稿
     */
    @Transactional
    public TournamentIdDTO create(TournamentCreateCmd cmd) {
        Tournament tournament = tournamentAdminService.create(cmd);
        return new TournamentIdDTO(tournament.getTournamentId());
    }

    /**
     * 编辑草稿
     */
    @Transactional
    public void update(TournamentUpdateCmd cmd) {
        tournamentAdminService.update(cmd);
    }

    /**
     * 激活赛事
     */
    @Transactional
    public void activate(TournamentActivateCmd cmd) {
        tournamentAdminService.activate(cmd.getTournamentId());
    }

    /**
     * 废弃赛事
     */
    @Transactional
    public void abandon(TournamentAbandonCmd cmd) {
        tournamentAdminService.abandon(cmd.getTournamentId());
    }

    /**
     * 后台赛事列表
     */
    public PageDTO<TournamentAdminItemDTO> list(TournamentAdminListCmd cmd) {
        PageDTO<TournamentData> pageData = tournamentAdminService.pageList(cmd);
        return new PageDTO<>(TournamentAppConvertMapper.INSTANCE.toTournamentAdminItemDTOList(pageData.getList()), pageData.getTotal(), pageData.getHasMore());
    }
}
