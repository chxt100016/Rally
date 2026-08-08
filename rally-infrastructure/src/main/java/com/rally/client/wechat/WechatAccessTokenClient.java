package com.rally.client.wechat;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.config.WechatAppProperties;
import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatAccessTokenClient {

    private final WechatAppProperties properties;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public String getAccessToken() {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now < tokenExpireAt) {
            return cachedToken;
        }
        synchronized (this) {
            now = System.currentTimeMillis() / 1000;
            if (cachedToken != null && now < tokenExpireAt) {
                return cachedToken;
            }
            AccessTokenResponse resp = Http.uri(properties.getAccessTokenUrl()).param("grant_type", "client_credential").param("appid", properties.getAppId()).param("secret", properties.getSecret()).sensitive().doGet().result(AccessTokenResponse.class);
            if (resp == null || resp.getErrcode() != 0 || StringUtils.isBlank(resp.getAccessToken())) {
                log.error("获取微信access_token失败: errcode={}, errmsg={}", resp != null ? resp.getErrcode() : -1, resp != null ? resp.getErrmsg() : "null");
                return null;
            }
            cachedToken = resp.getAccessToken();
            tokenExpireAt = now + Math.max(60, resp.getExpiresIn() - 300);
            return cachedToken;
        }
    }

    @Data
    private static class AccessTokenResponse {
        @JSONField(name = "access_token")
        private String accessToken;
        @JSONField(name = "expires_in")
        private long expiresIn;
        private int errcode;
        private String errmsg;
    }
}
