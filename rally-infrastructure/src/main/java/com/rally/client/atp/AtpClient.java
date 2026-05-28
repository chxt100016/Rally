package com.rally.client.atp;

import com.alibaba.fastjson2.JSON;
import com.rally.client.atp.model.AtpAppCompletedResponse;
import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.wta.model.WtaScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AtpClient {

    private static final String RANKINGS_URL =
            "https://app.atptour.com/api/v2/gateway/rankings/sglroll";

    private static final String DRAWS_URL =
            "https://app.atptour.com/api/v2/gateway/draws/ms";

    private static final String LIVE_MATCHES_URL =
            "https://app.atptour.com/api/v2/gateway/livematches";

    private static final String SCHEDULE_URL =
            "https://app.atptour.com/api/v2/gateway/scores/schedule";

    private static final String COMPLETED_URL =
            "https://app.atptour.com/api/v2/gateway/results/completed";

    private final FlareSolverrClient flareSolver;

    public AtpClient(FlareSolverrClient flareSolver) {
        this.flareSolver = flareSolver;
    }

    public AtpRankingsResponse getRankings(int fromRank, int toRank) {
        try {
            String url = RANKINGS_URL
                    + "?fromRank=" + fromRank
                    + "&toRank=" + toRank
                    + "&language=en";
            String json = flareSolver.get(url);
            return json == null ? null : JSON.parseObject(json, AtpRankingsResponse.class);
        } catch (Exception e) {
            log.error("获取ATP排名失败, fromRank={}, toRank={}", fromRank, toRank, e);
            return null;
        }
    }

    public AtpAppDrawResponse getDraws(String eventId, int eventYear) {
        try {
            String url = DRAWS_URL
                    + "?eventId=" + eventId
                    + "&eventYear=" + eventYear;
            String json = flareSolver.get(url);
            return json == null ? null : JSON.parseObject(json, AtpAppDrawResponse.class);
        } catch (Exception e) {
            log.error("获取ATP签表失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }

    public AtpAppLiveResponse getLiveMatches(String eventId, int eventYear) {
        try {
            String url = LIVE_MATCHES_URL
                    + "?eventid=" + eventId
                    + "&eventYear=" + eventYear;
            String json = flareSolver.get(url);
            return json == null ? null : JSON.parseObject(json, AtpAppLiveResponse.class);
        } catch (Exception e) {
            log.error("获取ATP实时比赛失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }

    public AtpAppCompletedResponse getCompleted(String eventId, int eventYear) {
        try {
            String url = COMPLETED_URL
                    + "?eventId=" + eventId
                    + "&eventYear=" + eventYear;
            String json = flareSolver.get(url);
            return json == null ? null : JSON.parseObject(json, AtpAppCompletedResponse.class);
        } catch (Exception e) {
            log.error("获取ATP已完成比赛失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }

    public WtaScheduleResponse getSchedule(String eventId, int eventYear) {
        try {
            String url = SCHEDULE_URL
                    + "?eventId=" + eventId
                    + "&eventYear=" + eventYear;
            String json = flareSolver.get(url);
            return json == null ? null : JSON.parseObject(json, WtaScheduleResponse.class);
        } catch (Exception e) {
            log.error("获取ATP赛程失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }
}
