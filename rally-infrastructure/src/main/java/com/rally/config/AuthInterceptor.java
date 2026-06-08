package com.rally.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.model.TokenPayload;
import com.rally.domain.tennis.model.Result;
import com.rally.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            writeError(response, BizErrorCode.TOKEN_EXPIRED);
            return false;
        }

        String token = authHeader.substring(7);
        Optional<TokenPayload> payload = TokenUtils.verify(token);
        if (payload.isEmpty()) {
            writeError(response, BizErrorCode.TOKEN_INVALID);
            return false;
        }

        com.rally.utils.UserContext.set(payload.get().getUserId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        com.rally.utils.UserContext.clear();
    }

    private void writeError(HttpServletResponse response, BizErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<Void> result = Result.fail(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
