package com.rally.protourdata.tournamentcatalogcollect.activity;

import com.rally.client.tourtv.AtpTvClient;
import com.rally.client.tourtv.model.MatchesResponse;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.tour.convert.TournamentAppConvertMapper;
import com.rally.tour.model.Tournament;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 collect-atp-tournament-catalog：采集并刷新指定年份 ATP 赛事名录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectAtpTournamentCatalogActivity {

    private static final String ATP = "ATP";

    private final AtpTvClient atpTvClient;
    private final TourTournamentRepository tournamentRepository;

    /**
     * @return 本批实际交给仓储的唯一赛事年度数；对外 HTTP 入口仍返回空响应体
     */
    public int execute(int year) {
        // A1：客户端固定按全年区间、size=200 请求 Tennis TV。
        List<MatchesResponse.TournamentInfo> sourceTournaments =
                atpTvClient.getTournaments(year);
        if (sourceTournaments != null && sourceTournaments.isEmpty()) {
            log.warn("从API获取赛事列表为空, year={}", year);
            return 0;
        }

        // A2：沿用 main 的映射与解析容错；来源未给年份时归入本次请求年份。
        Map<String, TournamentData> deduplicated = new LinkedHashMap<>();
        for (MatchesResponse.TournamentInfo source : sourceTournaments) {
            Tournament tournament = TournamentAppConvertMapper.INSTANCE.toTournament(source);
            tournament.setTour(ATP);
            if (tournament.getYear() == null) {
                tournament.setYear(year);
            }

            TournamentData data = TournamentAppConvertMapper.INSTANCE.toTournamentData(tournament);
            if (data.getTournamentId() == null || data.getTournamentId().isBlank()) {
                continue;
            }
            deduplicated.put(identityKey(data), data);
        }

        // A3：空批次不触发仓储；其余按 tournamentId+year 新增或刷新。
        // 仓储的非空列更新保留来源缺失值，且不写图片列、不删除历史赛事。
        if (deduplicated.isEmpty()) {
            return 0;
        }
        List<TournamentData> tournaments = new ArrayList<>(deduplicated.values());
        tournamentRepository.saveOrUpdateBatch(tournaments);
        log.info("ATP赛事采集完成: year={}, 数量={}", year, tournaments.size());
        return tournaments.size();
    }

    private String identityKey(TournamentData tournament) {
        return tournament.getTournamentId() + "_" + tournament.getYear();
    }
}
