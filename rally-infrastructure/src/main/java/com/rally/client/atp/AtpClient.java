package com.rally.client.atp;

import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.domain.utils.Http;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AtpClient {

    private static final String RANKINGS_URL =
            "https://app.atptour.com/api/v2/gateway/rankings/sglroll";

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
}
