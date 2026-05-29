package com.rally.web.auth;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.AuthException;

public class UserContext {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    public static void set(String userId) {
        HOLDER.set(userId);
    }

    /**
     * 获取当前用户 ID，未登录时抛出 AuthException
     */
    public static String get() {
        String userId = HOLDER.get();
        if (userId == null) {
            throw new AuthException(BizErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 获取当前用户 ID，未登录时返回 null（用于可选场景）
     */
    public static String getIfPresent() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
