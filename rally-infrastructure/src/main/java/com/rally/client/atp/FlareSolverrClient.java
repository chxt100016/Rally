package com.rally.client.atp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * FlareSolverr 代理客户端，用于绕过 Cloudflare 人机验证。
 * 通过本地 FlareSolverr 服务（Docker）发起请求，自动解决 JS 挑战。
 */
@Slf4j
@Component
public class FlareSolverrClient {

    private static final String FLARESOLVERR_URL = "http://localhost:8191/v1";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 通过 FlareSolverr 发起 GET 请求，返回响应体 JSON 字符串
     *
     * @param targetUrl 完整的目标 URL（含查询参数）
     * @return 响应体字符串，失败返回 null
     */
    public String get(String targetUrl) {
        try {
            // 构造 FlareSolverr 请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("cmd", "request.get");
            requestBody.put("url", targetUrl);
            requestBody.put("maxTimeout", 60000);

            String body = requestBody.toJSONString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FLARESOLVERR_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseStr = httpResponse.body();

            if (responseStr == null) {
                log.error("FlareSolverr 无响应, url={}", targetUrl);
                return null;
            }

            JSONObject flareResponse = JSON.parseObject(responseStr);
            String status = flareResponse.getString("status");

            if (!"ok".equals(status)) {
                log.error("FlareSolverr 请求失败, status={}, message={}, url={}",
                        status, flareResponse.getString("message"), targetUrl);
                return null;
            }

            JSONObject solution = flareResponse.getJSONObject("solution");
            int httpStatus = solution.getIntValue("status");

            if (httpStatus != 200) {
                log.error("FlareSolverr 目标返回非200, status={}, url={}", httpStatus, targetUrl);
                return null;
            }

            // FlareSolverr 浏览器会将 API JSON 响应包裹在 <pre> 标签中，需要提取纯 JSON
            String response = solution.getString("response");
            return extractJson(response);

        } catch (Exception e) {
            log.error("FlareSolverr 请求异常, url={}", targetUrl, e);
            return null;
        }
    }

    /**
     * 从 FlareSolverr 返回的 HTML 中提取 JSON 内容。
     * API 响应会被浏览器包裹在 &lt;pre&gt;...&lt;/pre&gt; 标签中。
     */
    private String extractJson(String response) {
        if (response == null) return null;
        // 如果是纯 JSON，直接返回
        String trimmed = response.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        // 提取 <pre> 标签中的内容
        int preStart = trimmed.indexOf("<pre>");
        int preEnd = trimmed.lastIndexOf("</pre>");
        if (preStart >= 0 && preEnd > preStart) {
            return trimmed.substring(preStart + 5, preEnd).trim();
        }
        return trimmed;
    }
}
