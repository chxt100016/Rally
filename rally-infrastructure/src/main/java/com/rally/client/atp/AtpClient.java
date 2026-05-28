package com.rally.client.atp;

import com.alibaba.fastjson2.JSON;
import com.rally.client.atp.model.AtpAppCompletedResponse;
import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.wta.model.WtaScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

    private final FlareSolverrClient flareSolverr;

    /** 缓存的 CookieManager，FlareSolverr 解决后写入，后续请求复用 */
    private volatile CookieManager cookieManager;
    /** 缓存的 userAgent，必须和 cookie 配套使用 */
    private volatile String userAgent = DEFAULT_USER_AGENT;

    public AtpClient(FlareSolverrClient flareSolverr) {
        this.flareSolverr = flareSolverr;
    }

    public AtpRankingsResponse getRankings(int fromRank, int toRank) {
        String url = RANKINGS_URL
                + "?fromRank=" + fromRank + "&toRank=" + toRank + "&language=en";
        String json = doGet(url);
        return json == null ? null : JSON.parseObject(json, AtpRankingsResponse.class);
    }

    public AtpAppDrawResponse getDraws(String eventId, int eventYear) {
        String url = DRAWS_URL
                + "?eventId=" + eventId + "&eventYear=" + eventYear;
        String json = doGet(url);
        return json == null ? null : JSON.parseObject(json, AtpAppDrawResponse.class);
    }

    public AtpAppLiveResponse getLiveMatches(String eventId, int eventYear) {
        String url = LIVE_MATCHES_URL
                + "?eventid=" + eventId + "&eventYear=" + eventYear;
        String json = doGet(url);
        return json == null ? null : JSON.parseObject(json, AtpAppLiveResponse.class);
    }

    public AtpAppCompletedResponse getCompleted(String eventId, int eventYear) {
        String url = COMPLETED_URL
                + "?eventId=" + eventId + "&eventYear=" + eventYear;
        String json = doGet(url);
        return json == null ? null : JSON.parseObject(json, AtpAppCompletedResponse.class);
    }

    public WtaScheduleResponse getSchedule(String eventId, int eventYear) {
        String url = SCHEDULE_URL
                + "?eventId=" + eventId + "&eventYear=" + eventYear;
        String json = doGet(url);
        return json == null ? null : JSON.parseObject(json, WtaScheduleResponse.class);
    }

    /**
     * 核心请求逻辑：先直连，遇到 Cloudflare 拦截时按需调用 FlareSolverr，缓存 cookie 后复用
     */
    private String doGet(String url) {
        try {
            // 第一步：尝试普通 HTTP 请求（带缓存的 cookie）
            String body = doDirectGet(url);
            if (body != null && !isChallengePage(body)) {
                return body;
            }

            // 第二步：被 Cloudflare 拦截，按需调用 FlareSolverr
            log.info("检测到 Cloudflare 拦截，调用 FlareSolverr: {}", url);
            FlareSolverrClient.FlareSolution solution = flareSolverr.solve(url);
            if (solution == null) {
                log.error("FlareSolverr 解决失败, url={}", url);
                return null;
            }

            // 缓存 cookie 和 userAgent，后续请求直接复用
            this.cookieManager = solution.getCookieManager();
            this.userAgent = solution.getUserAgent();
            return solution.getBody();

        } catch (Exception e) {
            log.error("ATP 请求异常, url={}", url, e);
            return null;
        }
    }

    /**
     * 用 java.net.http.HttpClient 发起普通 GET 请求，支持自动携带 cookie
     */
    private String doDirectGet(String url) throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        // 如果有缓存的 cookie，使用带 cookie 的 CookieManager
        if (cookieManager != null) {
            builder.cookieHandler(cookieManager);
        }

        HttpClient client = builder.build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 判断响应是否为 Cloudflare 拦截页面
     */
    private boolean isChallengePage(String body) {
        if (body == null) return false;
        return body.contains("Just a moment...") || body.contains("_cf_chl_opt");
    }
}
