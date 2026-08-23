package com.rally.web.config;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 保护仅供运营后台调用的 Java 接口。 */
@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "X-Admin-Key";

    private final String configuredKey;

    public AdminApiKeyInterceptor(@Value("${admin.api-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String suppliedKey = request.getHeader(HEADER_NAME);
        if (StringUtils.isBlank(configuredKey) || StringUtils.isBlank(suppliedKey)) {
            throw new AuthException(BizErrorCode.ACCESS_DENIED);
        }
        boolean matches = MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw new AuthException(BizErrorCode.ACCESS_DENIED);
        }
        return true;
    }
}
