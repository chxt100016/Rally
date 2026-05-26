package com.rally.client.atp;

import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.domain.utils.Http;
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

    public AtpRankingsResponse getRankings(int fromRank, int toRank) {
        try {
            return Http.uri(RANKINGS_URL)
                    .param("fromRank", String.valueOf(fromRank))
                    .param("toRank", String.valueOf(toRank))
                    .param("language", "en")
                    .header("Host", "app.atptour.com")
                    .header("accept", "application/json")
                    .header("user-agent", "ATPTourApp")
                    .header("accept-language", "zh-CN,zh-Hans;q=0.9")
                    .doGet()
                    .result(AtpRankingsResponse.class);
        } catch (Exception e) {
            log.error("获取ATP排名失败, fromRank={}, toRank={}", fromRank, toRank, e);
            return null;
        }
    }

    public AtpAppDrawResponse getDraws(String eventId, int eventYear) {
        try {
            return Http.uri(DRAWS_URL)
                    .param("eventId", eventId)
                    .param("eventYear", String.valueOf(eventYear))
                    .header("Host", "app.atptour.com")
                    .header("accept", "application/json")
                    .header("user-agent", "ATPTourApp")
                    .header("accept-language", "zh-CN,zh-Hans;q=0.9")
                    .doGet()
                    .result(AtpAppDrawResponse.class);
        } catch (Exception e) {
            log.error("获取ATP签表失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }

    public AtpAppLiveResponse getLiveMatches(String eventId, int eventYear) {
        try {
            return Http.uri(LIVE_MATCHES_URL)
                    .param("eventid", eventId)
                    .param("eventYear", String.valueOf(eventYear))
                    .header("Host", "app.atptour.com")
                    .header("accept", "application/json")
                    .header("user-agent", "ATPTourApp")
                    .header("accept-language", "zh-CN,zh-Hans;q=0.9")
                    .doGet()
                    .result(AtpAppLiveResponse.class);
        } catch (Exception e) {
            log.error("获取ATP实时比赛失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }

    public WtaScheduleResponse getSchedule(String eventId, int eventYear) {
        try {
            return Http.uri(SCHEDULE_URL)
                    .param("eventId", eventId)
                    .param("eventYear", String.valueOf(eventYear))
                    .header("Host", "app.atptour.com")
                    .header("accept", "application/json")
                    .header("user-agent", "ATPTourApp")
                    .header("accept-language", "zh-CN,zh-Hans;q=0.9")
                    .doGet()
                    .result(WtaScheduleResponse.class);
        } catch (Exception e) {
            log.error("获取ATP赛程失败, eventId={}, eventYear={}", eventId, eventYear, e);
            return null;
        }
    }
}
