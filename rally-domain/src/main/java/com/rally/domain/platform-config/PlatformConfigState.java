package com.rally.domain.system.platformconfig;

/** 与 sys_config 单行对应的不可变聚合状态。 */
public record PlatformConfigState(
        Long id,
        String bizId,
        ConfigIdentity identity,
        TypedConfigValue value,
        String description,
        boolean enabled,
        int version) {

    public static PlatformConfigState firstPublished(
            String bizId,
            ConfigIdentity identity,
            TypedConfigValue value,
            String description) {
        return new PlatformConfigState(
                null, bizId, identity, value, description, true, 1);
    }

    public PlatformConfigState withGeneratedId(long generatedId) {
        return new PlatformConfigState(
                generatedId, bizId, identity, value, description, enabled, version);
    }

    public PlatformConfigState published(
            TypedConfigValue nextValue, String nextDescription) {
        return new PlatformConfigState(
                id, bizId, identity, nextValue, nextDescription, true, version + 1);
    }

    public PlatformConfigState disabled() {
        return new PlatformConfigState(
                id, bizId, identity, value, description, false, version + 1);
    }

    public String configKey() {
        return identity == null ? null : identity.configKey();
    }

    public String scope() {
        return identity == null ? null : identity.scope();
    }

    public String valueType() {
        return value == null ? null : value.valueType();
    }

    public String configValue() {
        return value == null ? null : value.normalizedValue();
    }
}
