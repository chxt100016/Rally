package com.rally.client.tennistv;

import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.tennistv.model.AtpOopResponse;
import com.rally.domain.utils.Http;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TennisTvClient {

    private static final String BASE_URI = "https://api.tennistv.com/tennis/v1";

    /**
     * 接口1: 查询最近比赛 (status=L 表示进行中)
     */
    public MatchesResponse getLiveMatches() {
        try {
            Http http = Http.uri(BASE_URI + "/matches")
                    .param("status", "L")
                    .header("origin", "https://www.tennistv.com")
                    .doGet();

            return http.result(MatchesResponse.class);
        } catch (Exception e) {
            log.error("获取最近比赛失败", e);
            return null;
        }
    }

    /**
     * 接口1.1: 按状态查询比赛 (如 status=P 表示正在进行)
     */
    public MatchesResponse getMatchesByStatus(String status) {
        try {
            Http http = Http.uri(BASE_URI + "/matches")
                    .param("status", status)
                    .header("origin", "https://www.tennistv.com")
                    .doGet();
            return http.result(MatchesResponse.class);
        } catch (Exception e) {
            log.error("按状态查询比赛失败, status={}", status, e);
            return null;
        }
    }

    /**
     * 接口2: 查询签表数据
     */
    public AtpDrawsResponse getDraws(String tournamentId, int year) {
        try {
            return Http.uri(BASE_URI + "/tournaments/" + tournamentId + "/" + year + "/draws")
                    .doGet()
                    .result(AtpDrawsResponse.class);
        } catch (Exception e) {
            log.error("获取签表数据失败, tournamentId={}, year={}", tournamentId, year, e);
            return null;
        }
    }

    /**
     * 接口2.5: 查询赛事列表
     */
    public List<MatchesResponse.TournamentInfo> getTournaments(int year) {
        try {
            return Http.uri(BASE_URI + "/tournaments")
                    .param("from", year + "-01-01")
                    .param("to", year + "-12-31")
                    .param("size", "200")
                    .header("origin", "https://www.tennistv.com")
                    .doGet()
                    .resultArray(MatchesResponse.TournamentInfo.class);
        } catch (Exception e) {
            log.error("获取赛事列表失败, year={}", year, e);
            return null;
        }
    }

    /**
     * 接口3: 查询比赛详情 (每日安排)
     */
    public List<AtpOopResponse> getOop() {
        try {
            Http http = Http.uri(BASE_URI + "/oop")
                    .doGet();
            return http.resultArray(AtpOopResponse.class);
        } catch (Exception e) {
            log.error("获取比赛详情失败", e);
            return null;
        }
    }
}
