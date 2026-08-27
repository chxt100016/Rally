package com.rally.tournament.tournamentlist.activity;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.model.TournamentAdminItemDTO;
import com.rally.domain.tournament.model.TournamentAdminListCmd;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.tournament.convert.TournamentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-tournament-admin-list：按后台筛选条件分页交付赛事摘要。
 */
@Component
@RequiredArgsConstructor
public class QueryTournamentAdminListActivity {

    private final TournamentAdminService tournamentAdminService;

    public PageDTO<TournamentAdminItemDTO> execute(TournamentAdminListCmd command) {
        // A1/A2：沿用主线的非空白精确筛选、create_time 倒序与页码分页语义。
        PageDTO<TournamentData> page = tournamentAdminService.pageList(command);

        // A3/A4：保留后台 DTO 字段与 null 语义，并对两个非空图片键生成 3600 秒签名地址。
        return new PageDTO<>(
                TournamentAppConvertMapper.INSTANCE.toTournamentAdminItemDTOList(page.getList()),
                page.getTotal(),
                page.getHasMore());
    }
}
