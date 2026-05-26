package com.rally.client.atp;

import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.atp.model.AtpTournamentsResponse;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.domain.utils.Http;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AtpClient {

    private static final String TOURNAMENTS_URL =
            "https://www.atptour.com/en/-/tournaments/calendar/tour";

    public AtpTournamentsResponse getTournaments(int year) {
        try {
            return Http.uri(TOURNAMENTS_URL)
                    .param("year", String.valueOf(year))
                    .header("accept", "application/json, text/plain, */*")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("if-modified-since", "Mon, 25 May 2026 08:42:17 GMT")
                    .header("priority", "u=1, i")
                    .header("referer", "https://www.atptour.com/en/tournaments")
                    .header("sec-ch-ua", "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"")
                    .header("sec-ch-ua-arch", "\"arm\"")
                    .header("sec-ch-ua-bitness", "\"64\"")
                    .header("sec-ch-ua-full-version", "\"148.0.7778.168\"")
                    .header("sec-ch-ua-full-version-list", "\"Chromium\";v=\"148.0.7778.168\", \"Google Chrome\";v=\"148.0.7778.168\", \"Not/A)Brand\";v=\"99.0.0.0\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-model", "\"\"")
                    .header("sec-ch-ua-platform", "\"macOS\"")
                    .header("sec-ch-ua-platform-version", "\"26.3.0\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
                    .header("Cookie", "_gcl_au=1.1.2127594075.1778678611; _ga=GA1.1.348842626.1778678613; OptanonAlertBoxClosed=2026-05-13T13:23:38.859Z; __exponea_etc__=72a64741-81c4-4ab4-93f7-91d259852a60; _tt_enable_cookie=1; _ttp=01KRGR3RR9B432P2CAHGZZ023C_.tt.1; __exponea_time2__=-0.004813432693481445; minVersion={\"experiment\":-1458757693,\"minFlavor\":\"domadaptermi-1.17.1.340.js100\"}; minUnifiedSessionToken10=%7B%22sessionId%22%3A%22bc1eb3111e-6f0c9b131e-c32d1aa4cf-2dcec4c0d2-9f5b6c23b7%22%2C%22uid%22%3A%22bf04b01d57-cdfceddb2d-718427afff-40be0a1b38-3a076d8857%22%2C%22__sidts__%22%3A1779698966093%2C%22__uidts__%22%3A1779698966093%7D; cf_clearance=DfNvz.sbLazizzX6kgFben8r9YKt00tS.yNEUMy3LOs-1779700876-1.2.1.1-luWwQK1sXvFKD8s7CI1KZqPhi5.K80QhbeL12N43dhnfOVB63HQH67f_Vaytth1iTHkDRL3zm4wiMzVmGE936HKruFMeLI2iFc1Bj1f_s1LgjYKzmenuR8LDvSYIZ2L2tFAGB0kNs92auRydWa_qc4DHTX1a2gZujDxeTojHGgm8GuWzp3uVf5arPIkXo4GtUIrb2stLPqBBZjMBuh5IqpwhwnsgVJ7PdKF1xnfViT.bz0vOyvuLwRP3PmzDYc0fzet4YzxcMMUk7L6pHGT9Xi8i1UzkIj2rU_EnD51Uk6TGcFBC5QBfz3GiPQvObjDHjAOcbX_j13eLIqJmmdEDsYNjsP3feVFbS3EJLOFaf_nthmxEauY6gOGXPKD6oKzzYKNZocc2.e4OBkiHg1zNgzKzrZ1DXcr49X3uoEmbpLA; __cf_bm=Qznxh650mGBmGHFJOiv9d6bhiIyCetp2HkLNowFcXcQ-1779700876.836077-1.0.1.1-f07IoQ6Od20jKQJUxPunuu0.gfGajIx2gqmJX3pyqtTEWhwLKwQ61.Npe_QIw5hZoD4G8VOgW3xtXl7iJKWZ50.H.r5E4OIEhEP3vR2b1TLHxWnGgwzWZruwA.aE3g_H; ttcsid_CES2GFBC77UAS1JKFA70=1779700870486::TNW5PD6ewVMHCW0pvV9n.3.1779700914828.1; ttcsid=1779700870486::JCe8AkFzYZAJwhH3nMOS.3.1779700913473.0::1.127003.0::42971.3.255.2780::39104.20.2069; _ga_D7VPPXYD0V=GS2.1.s1779700869$o4$g1$t1779701004$j53$l0$h0; OptanonConsent=isGpcEnabled=0&datestamp=Mon+May+25+2026+17%3A23%3A24+GMT%2B0800+(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)&version=202603.1.0&browserGpcFlag=0&isIABGlobal=false&hosts=&consentId=e1ed33ce-1047-4527-8459-76360a546b7e&interactionCount=1&isAnonUser=1&prevHadToken=0&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0004%3A1%2CC0003%3A1&intType=1&crTime=1778678619725&geolocation=CN%3BZJ&AwaitingReconsent=false; __cf_bm=nsc_94Xhcczk0Vb7QuzuG.PZhdegWHU7idwsEPclGrc-1779707088.859663-1.0.1.1-u.rM3lBEUhSHKcp8QFBkztFq.5MIM3fiRG6h.zXr9E0r9p4aJcGqloHOeiFYZJx1OJdPFQTeQlncWkiZdMURqvgcTSswqsrGW5MKbWMIcyt134Ha2m2WigKoUAj24sjJ")
                    .doGet()
                    .result(AtpTournamentsResponse.class);
        } catch (Exception e) {
            log.error("获取ATP赛事列表失败, year={}", year, e);
            return null;
        }
    }

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
