package com.rally.client.wta;

import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.client.wta.model.WtaRankingsResponse;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.domain.utils.Http;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WtaClient {

    private static final String BASE_URI = "https://api.wtatennis.com/tour/tournaments";

    public WtaTournamentsResponse getTournaments(int year) {
        try {
            return Http.uri(BASE_URI)
                    .param("page", "0")
                    .param("pageSize", "1000")
                    .param("excludeLevels", "ITF")
                    .param("from", year + "-01-01")
                    .param("to", year + "-12-31")
                    .doGet()
                    .result(WtaTournamentsResponse.class);
        } catch (Exception e) {
            log.error("获取WTA赛事列表失败, year={}", year, e);
            return null;
        }
    }

    public WtaMatchesResponse getMatches(String tournamentId, int year) {
        try {
            String url = BASE_URI + "/" + tournamentId + "/" + year + "/matches";
            return Http.uri(url).doGet().result(WtaMatchesResponse.class);
        } catch (Exception e) {
            log.error("获取WTA签表失败, tournamentId={}, year={}", tournamentId, year, e);
            return null;
        }
    }

    public WtaDrawsResponse getDraws(String tournamentId, int year) {
        try {
            String url = "https://wta-webapi-prod-apimanagement.azure-api.net/atpjoint-api/v1/TournamentDraws/draws";
            return Http.uri(url)
                    .param("eventYear", String.valueOf(year))
                    .param("eventId", tournamentId)
                    .param("drawType", "ls")
                    .param("api-version", "1")
                    .header("apikey", "8334323343164715938a39449ac5bb69")
                    .doGet()
                    .result(WtaDrawsResponse.class);
        } catch (Exception e) {
            log.error("获取WTA签表失败, tournamentId={}, year={}", tournamentId, year, e);
            return null;
        }
    }

    public WtaMatchesResponse getLiveMatches(String tournamentId, int year) {
        try {
            String url = BASE_URI + "/" + tournamentId + "/" + year + "/matches";
            return Http.uri(url).param("states", "L").doGet().result(WtaMatchesResponse.class);
        } catch (Exception e) {
            log.error("获取WTA进行中比赛失败, tournamentId={}, year={}", tournamentId, year, e);
            return null;
        }
    }

    public WtaScheduleResponse getSchedule(String tournamentId, int year) {
        try {
            String url = "https://wta-webapi-prod-apimanagement.azure-api.net/atpjoint-api/v1/Scores/Schedule";
            return Http.uri(url)
                    .param("eventId", tournamentId)
                    .param("eventYear", String.valueOf(year))
                    .header("apikey", "8334323343164715938a39449ac5bb69")
                    .doGet()
                    .result(WtaScheduleResponse.class);
        } catch (Exception e) {
            log.error("获取WTA赛程失败, tournamentId={}, year={}", tournamentId, year, e);
            return null;
        }
    }

    public WtaRankingsResponse getRankings(int fromRank, int toRank) {
        try {
            String url = "https://wta-webapi-prod-apimanagement.azure-api.net/atpjoint-api/v1/Rankings/sglroll";
            return Http.uri(url)
                    .param("fromRank", String.valueOf(fromRank))
                    .param("toRank", String.valueOf(toRank))
                    .param("api-version", "1.0")
                    .header("apikey", "8334323343164715938a39449ac5bb69")
                    .doGet()
                    .result(WtaRankingsResponse.class);
        } catch (Exception e) {
            log.error("获取WTA排名失败, fromRank={}, toRank={}", fromRank, toRank, e);
            return null;
        }
    }
}
