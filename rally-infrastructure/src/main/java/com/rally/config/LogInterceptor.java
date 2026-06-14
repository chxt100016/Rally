package com.rally.config;

import com.rally.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component

public class LogInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        START_TIME.set(System.currentTimeMillis());
        String userId = UserContext.getIfPresent();
        if (userId != null) {
            log.info("[REQ] IP={} userId={} {} {}", getClientIp(request), userId, request.getMethod(), request.getRequestURI());
        } else {
            log.info("[REQ] IP={} {} {}", getClientIp(request), request.getMethod(), request.getRequestURI());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long start = START_TIME.get();
        if (start != null) {
            String userId = UserContext.getIfPresent();
            if (userId != null) {
                log.info("[RES] IP={} userId={} {} {} cost={}ms", getClientIp(request), userId, request.getMethod(), request.getRequestURI(), System.currentTimeMillis() - start);
            } else {
                log.info("[RES] IP={} {} {} cost={}ms", getClientIp(request), request.getMethod(), request.getRequestURI(), System.currentTimeMillis() - start);
            }
            START_TIME.remove();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
