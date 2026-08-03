package com.rally.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Marks whether the request body exceeded the behavior-log cache limit.
 */
public class UserBehaviorRequestWrapper extends ContentCachingRequestWrapper {

    private boolean overflow;

    public UserBehaviorRequestWrapper(HttpServletRequest request, int cacheLimit) {
        super(request, cacheLimit);
    }

    @Override
    protected void handleContentOverflow(int contentCacheLimit) {
        overflow = true;
    }

    public boolean isOverflow() {
        return overflow;
    }
}
