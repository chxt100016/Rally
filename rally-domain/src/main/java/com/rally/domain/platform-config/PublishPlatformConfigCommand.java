package com.rally.domain.system.platformconfig;

import java.util.Objects;

/** C1 发布配置命令；工厂方法完成首次规范化和名录校验。 */
public final class PublishPlatformConfigCommand {

    private final PlatformConfigDefinition definition;
    private final ConfigIdentity identity;
    private final TypedConfigValue value;
    private final String description;
    private final int expectedVersion;

    private PublishPlatformConfigCommand(
            PlatformConfigDefinition definition,
            ConfigIdentity identity,
            TypedConfigValue value,
            String description,
            int expectedVersion) {
        this.definition = definition;
        this.identity = identity;
        this.value = value;
        this.description = description;
        this.expectedVersion = expectedVersion;
    }

    public static PublishPlatformConfigCommand prepare(
            PlatformConfigDefinition definition,
            String scope,
            String rawValue,
            String description,
            int expectedVersion) {
        require(definition != null,
                PlatformConfig.CONFIG_VALUE_INVALID,
                "配置键不存在于名录");
        require(expectedVersion >= 0,
                PlatformConfig.CONFIG_VERSION_CONFLICT,
                "期望版本不能为负");
        String key;
        String valueType;
        String normalized;
        boolean accepted;
        try {
            key = definition.configKey();
            valueType = definition.valueType();
            normalized = definition.normalize(rawValue);
            accepted = definition.accepts(normalized);
        } catch (RuntimeException exception) {
            if (exception instanceof PlatformConfigDomainException domainException) {
                throw domainException;
            }
            throw new PlatformConfigDomainException(
                    PlatformConfig.CONFIG_VALUE_INVALID,
                    "配置值未通过名录规则校验",
                    exception);
        }
        require(accepted,
                PlatformConfig.CONFIG_VALUE_INVALID,
                "配置值未通过名录规则校验");

        return new PublishPlatformConfigCommand(
                definition,
                new ConfigIdentity(key, scope),
                new TypedConfigValue(valueType, normalized),
                description,
                expectedVersion);
    }

    public PlatformConfigDefinition definition() {
        return definition;
    }

    public ConfigIdentity identity() {
        return identity;
    }

    public TypedConfigValue value() {
        return value;
    }

    public String description() {
        return description;
    }

    public int expectedVersion() {
        return expectedVersion;
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new PlatformConfigDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
