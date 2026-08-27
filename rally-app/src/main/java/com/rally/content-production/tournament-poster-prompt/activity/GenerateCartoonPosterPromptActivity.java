package com.rally.contentproduction.tournamentposterprompt.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.utils.Assert;
import com.rally.tour.poster.PosterPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 generate-cartoon-poster-prompt：按职业赛事资料生成三维卡通风格海报提示词。
 */
@Component
@RequiredArgsConstructor
public class GenerateCartoonPosterPromptActivity {

    private final TourTournamentQueryDomainService tournamentQueryService;

    public String execute(String tournamentId) {
        // A1 沿用现有查询语义：只按赛事编号取仓储返回的第一条，不附加年份或排序。
        TournamentData tournament = tournamentQueryService.findByTournamentId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        // A2/A3/A4 复用现有组装器的主题、场地/级别、特色和固定收尾规则。
        return PosterPromptBuilder.build(tournament);
    }
}
