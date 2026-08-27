package com.rally.domain.system.platformconfig;

import java.util.Objects;

/** 由配置键和作用域共同构成的不可变配置身份。 */
public record ConfigIdentity(String configKey, String scope) {

    private static final int CONFIG_KEY_MAX = 128;
    private static final int SCOPE_MAX = 64;

    public ConfigIdentity {
        requireNotBlank(configKey, "配置键不能为空");
        requireNotBlank(scope, "配置作用域不能为空");
        require(configKey.length() <= CONFIG_KEY_MAX, "配置键长度不能超过 128");
        require(scope.length() <= SCOPE_MAX, "配置作用域长度不能超过 64");
    }

    private static void requireNotBlank(String value, String message) {
        require(value != null && !value.isBlank(), message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new PlatformConfigDomainException(
                    PlatformConfig.CONFIG_IDENTITY_CONFLICT,
                    Objects.requireNonNull(message));
        }
    }
}
