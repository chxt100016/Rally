package com.rally.protourdata.tournamentcatalogcollect.activity;

import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.tour.convert.WtaTournamentAppConvertMapper;
import com.rally.tour.model.Tournament;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 collect-wta-tournament-catalog：采集并刷新指定年份 WTA 赛事名录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectWtaTournamentCatalogActivity {

    private static final String WTA = "WTA";

    private final WtaClient wtaClient;
    private final TourTournamentRepository tournamentRepository;

    /**
     * @return 本批实际交给仓储的唯一赛事年度数；对外 HTTP 入口仍返回空响应体
     */
    public int execute(int year) {
        // A1：固定请求该年 WTA 第 0 页、最多 1000 条并排除 ITF。
        WtaTournamentsResponse response = wtaClient.getTournaments(year);
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            log.warn("从WTA API获取赛事列表为空, year={}", year);
            return 0;
        }

        // A2：沿用 main 的 WTA 状态、级别、奖金和日期映射，并强制 WTA 归属。
        // 来源未给有效年份时归入本次请求年份；空赛事 ID 不发出领域命令。
        Map<String, TournamentData> deduplicated = new LinkedHashMap<>();
        for (WtaTournamentsResponse.TournamentItem source : response.getContent()) {
            Tournament tournament = WtaTournamentAppConvertMapper.INSTANCE.toTournament(source);
            tournament.setTour(WTA);
            if (tournament.getYear() == null || tournament.getYear() == 0) {
                tournament.setYear(year);
            }

            TournamentData data = WtaTournamentAppConvertMapper.INSTANCE.toTournamentData(tournament);
            if (data.getTournamentId() == null || data.getTournamentId().isBlank()) {
                continue;
            }
            deduplicated.put(identityKey(data), data);
        }

        // A3：空批次不触发仓储；其余按 tournamentId+year 新增或刷新。
        // 仓储更新不写图片列，也不删除本次来源未出现的历史赛事。
        if (deduplicated.isEmpty()) {
            return 0;
        }
        List<TournamentData> tournaments = new ArrayList<>(deduplicated.values());
        tournamentRepository.saveOrUpdateBatch(tournaments);
        log.info("WTA赛事采集完成: year={}, 数量={}", year, tournaments.size());
        return tournaments.size();
    }

    private String identityKey(TournamentData tournament) {
        return tournament.getTournamentId() + "_" + tournament.getYear();
    }
}
