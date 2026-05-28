package com.rally.client.atp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * FlareSolverr 代理客户端，用于绕过 Cloudflare 人机验证。
 * 仅在普通 HTTP 请求被拦截时按需调用，获取 cf_clearance cookie 后缓存复用。
 */
@Slf4j
@Component
public class FlareSolverrClient {

    private static final String FLARESOLVERR_URL = "http://localhost:8191/v1";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 通过 FlareSolverr 解决 Cloudflare 挑战，返回包含 cookie 和 userAgent 的结果
     *
     * @param targetUrl 目标 URL
     * @return 解决结果，失败返回 null
     */
    public FlareSolution solve(String targetUrl) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("cmd", "request.get");
            requestBody.put("url", targetUrl);
            requestBody.put("maxTimeout", 60000);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FLARESOLVERR_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseStr = httpResponse.body();

            if (responseStr == null) {
                log.error("FlareSolverr 无响应, url={}", targetUrl);
                return null;
            }

            JSONObject flareResponse = JSON.parseObject(responseStr);
            if (!"ok".equals(flareResponse.getString("status"))) {
                log.error("FlareSolverr 请求失败, message={}, url={}",
                        flareResponse.getString("message"), targetUrl);
                return null;
            }

            JSONObject solution = flareResponse.getJSONObject("solution");
            if (solution.getIntValue("status") != 200) {
                log.error("FlareSolverr 目标返回非200, status={}, url={}",
                        solution.getIntValue("status"), targetUrl);
                return null;
            }

            // 提取响应体中的 JSON
            String body = extractJson(solution.getString("response"));
            String userAgent = solution.getString("userAgent");

            // 解析 FlareSolverr 返回的 cookie
            CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            JSONArray cookies = solution.getJSONArray("cookies");
            if (cookies != null) {
                for (int i = 0; i < cookies.size(); i++) {
                    JSONObject c = cookies.getJSONObject(i);
                    try {
                        HttpCookie cookie = new HttpCookie(c.getString("name"), c.getString("value"));
                        cookie.setDomain(c.getString("domain"));
                        cookie.setPath(c.getString("path"));
                        cookie.setHttpOnly(c.getBooleanValue("httpOnly"));
                        cookie.setSecure(c.getBooleanValue("secure"));
                        if (c.containsKey("expiry")) {
                            cookie.setMaxAge(c.getLong("expiry") - System.currentTimeMillis() / 1000);
                        }
                        cookieManager.getCookieStore().add(URI.create("https://" + c.getString("domain")), cookie);
                    } catch (Exception e) {
                        log.warn("解析cookie失败: {}", c, e);
                    }
                }
            }

            log.info("FlareSolverr 解决成功, cookies={}, url={}",
                    cookieManager.getCookieStore().getCookies().size(), targetUrl);
            return new FlareSolution(body, cookieManager, userAgent);

        } catch (Exception e) {
            log.error("FlareSolverr 请求异常, url={}", targetUrl, e);
            return null;
        }
    }

    /**
     * 从 FlareSolverr 返回的 HTML 中提取 JSON 内容
     */
    private String extractJson(String response) {
        if (response == null) return null;
        String trimmed = response.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        int preStart = trimmed.indexOf("<pre>");
        int preEnd = trimmed.lastIndexOf("</pre>");
        if (preStart >= 0 && preEnd > preStart) {
            return trimmed.substring(preStart + 5, preEnd).trim();
        }
        return trimmed;
    }

    /**
     * FlareSolverr 解决结果：响应体 + cookie 管理器 + userAgent
     */
    public static class FlareSolution {
        private final String body;
        private final CookieManager cookieManager;
        private final String userAgent;

        public FlareSolution(String body, CookieManager cookieManager, String userAgent) {
            this.body = body;
            this.cookieManager = cookieManager;
            this.userAgent = userAgent;
        }

        public String getBody() { return body; }
        public CookieManager getCookieManager() { return cookieManager; }
        public String getUserAgent() { return userAgent; }
    }
}
