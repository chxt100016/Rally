package com.rally.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Caches at most a small prefix of /wechat request bodies without consuming the
 * stream before controllers read it.
 */
@Component
public class UserBehaviorRequestCachingFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int maxParamsBytes;

    public UserBehaviorRequestCachingFilter(
            @Value("${behavior-log.enabled:true}") boolean enabled,
            @Value("${behavior-log.max-params-bytes:16384}") int maxParamsBytes) {
        this.enabled = enabled;
        this.maxParamsBytes = maxParamsBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return !enabled || !("/wechat".equals(servletPath) || servletPath.startsWith("/wechat/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request instanceof UserBehaviorRequestWrapper || isMultipart(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new UserBehaviorRequestWrapper(request, maxParamsBytes), response);
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }
}
