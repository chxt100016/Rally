package com.rally.domain.system.platformconfig;

import java.util.Objects;

/** 已经由配置名录规范化并验证的类型化字符串值。 */
public record TypedConfigValue(String valueType, String normalizedValue) {

    private static final int VALUE_TYPE_MAX = 8;
    private static final int CONFIG_VALUE_MAX = 100_000;

    public TypedConfigValue {
        require(valueType != null && !valueType.isBlank(),
                PlatformConfig.CONFIG_VALUE_INVALID,
                "配置值类型不能为空");
        require(valueType.length() <= VALUE_TYPE_MAX,
                PlatformConfig.CONFIG_VALUE_INVALID,
                "配置值类型长度不能超过 8");
        require(normalizedValue != null,
                PlatformConfig.CONFIG_VALUE_TOO_LONG,
                "规范化配置值不能为空");
        require(normalizedValue.length() <= CONFIG_VALUE_MAX,
                PlatformConfig.CONFIG_VALUE_TOO_LONG,
                "规范化配置值长度不能超过 100000");
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new PlatformConfigDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
