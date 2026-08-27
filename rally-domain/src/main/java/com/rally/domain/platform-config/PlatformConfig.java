package com.rally.domain.system.platformconfig;

import java.util.Objects;

/**
 * 平台配置聚合根。
 *
 * <p>一条 {@code config_key+scope} 记录是加载和保存单位。所有 {@code sys_config}
 * 写入只能经 C1/C2 调用 {@link PlatformConfigPersistence}；CAS 成功前不会推进内存状态。</p>
 */
public final class PlatformConfig {

    public static final String CONFIG_IDENTITY_CONFLICT = "CONFIG_IDENTITY_CONFLICT";
    public static final String CONFIG_VALUE_INVALID = "CONFIG_VALUE_INVALID";
    public static final String CONFIG_VALUE_TOO_LONG = "CONFIG_VALUE_TOO_LONG";
    public static final String CONFIG_VERSION_CONFLICT = "CONFIG_VERSION_CONFLICT";

    private static final int BIZ_ID_MAX = 32;
    private PlatformConfigState state;
    private final PlatformConfigDefinition definition;

    private PlatformConfig(
            PlatformConfigState state, PlatformConfigDefinition definition) {
        this.state = state;
        this.definition = definition;
    }

    /**
     * C1（记录不存在）：expectedVersion 必须为 0，建立 version=1 的 ENABLED 配置。
     * 数据库唯一键竞争通过插入结果统一转换成 I1 错误。
     */
    public static PlatformConfig firstPublish(
            PublishPlatformConfigCommand command,
            PlatformConfigIdGenerator idGenerator,
            PlatformConfigPersistence persistence) {
        require(command != null, CONFIG_VALUE_INVALID, "发布配置命令不能为空");
        require(idGenerator != null, CONFIG_IDENTITY_CONFLICT, "配置编号生成器不能为空");
        require(persistence != null, CONFIG_VERSION_CONFLICT, "配置持久化端口不能为空");
        require(command.expectedVersion() == 0,
                CONFIG_VERSION_CONFLICT,
                "首次发布期望版本必须为 0");
        validateAgainstDefinition(command.definition(), command.identity(), command.value());

        String bizId = idGenerator.nextBizId();
        requireNotBlank(bizId, CONFIG_IDENTITY_CONFLICT, "配置业务编号不能为空");
        require(bizId.length() <= BIZ_ID_MAX,
                CONFIG_IDENTITY_CONFLICT,
                "配置业务编号长度不能超过 32");

        PlatformConfig config = new PlatformConfig(
                PlatformConfigState.firstPublished(
                        bizId,
                        command.identity(),
                        command.value(),
                        command.description()),
                command.definition());
        config.checkInvariants();

        PlatformConfigInsertResult result = persistence.insert(config.state);
        require(result != null && result.outcome() != null,
                CONFIG_VERSION_CONFLICT,
                "配置插入没有返回有效结果");
        switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        CONFIG_IDENTITY_CONFLICT,
                        "配置插入未返回有效主键");
                config.state = config.state.withGeneratedId(result.generatedId());
                config.checkInvariants();
                return config;
            }
            case BIZ_ID_CONFLICT, IDENTITY_CONFLICT -> throw identityConflict();
            default -> throw error(CONFIG_VERSION_CONFLICT, "未知配置插入结果");
        }
    }

    /** 从一条完整数据库记录恢复聚合，并用当前应用名录重新校验键、类型和值。 */
    public static PlatformConfig restore(
            PlatformConfigState state, PlatformConfigDefinition definition) {
        require(state != null, CONFIG_IDENTITY_CONFLICT, "平台配置不存在");
        PlatformConfig config = new PlatformConfig(state, definition);
        config.checkInvariants();
        config.requirePersistentId();
        return config;
    }

    /** C1（记录已存在）：发布新值或重新启用，版本精确加一。 */
    public void publish(
            PublishPlatformConfigCommand command,
            PlatformConfigPersistence persistence) {
        require(command != null, CONFIG_VALUE_INVALID, "发布配置命令不能为空");
        requirePersistence(persistence);
        require(Objects.equals(state.identity(), command.identity()),
                CONFIG_IDENTITY_CONFLICT,
                "配置键和作用域建立后不可修改");
        require(Objects.equals(definition.configKey(), command.definition().configKey())
                        && Objects.equals(definition.valueType(), command.definition().valueType()),
                CONFIG_VALUE_INVALID,
                "发布命令使用的名录定义与已加载配置不一致");
        require(Objects.equals(state.value().valueType(), command.value().valueType()),
                CONFIG_VALUE_INVALID,
                "配置值类型建立后不可修改");
        require(command.expectedVersion() == state.version(),
                CONFIG_VERSION_CONFLICT,
                "发布配置期望版本与当前版本不一致");
        require(state.version() < Integer.MAX_VALUE,
                CONFIG_VERSION_CONFLICT,
                "配置版本已达到上限");
        validateAgainstDefinition(command.definition(), command.identity(), command.value());
        requirePersistentId();

        boolean updated = persistence.publishIfVersion(
                state.id(),
                command.expectedVersion(),
                command.value().normalizedValue(),
                command.description());
        require(updated, CONFIG_VERSION_CONFLICT, "发布配置版本竞争失败");
        state = state.published(command.value(), command.description());
        checkInvariants();
    }

    /** C2：仅 ENABLED 配置可按期望版本停用，原值保持不变。 */
    public void disable(int expectedVersion, PlatformConfigPersistence persistence) {
        requirePersistence(persistence);
        require(state.enabled(), CONFIG_VERSION_CONFLICT, "只有启用配置可以停用");
        require(expectedVersion == state.version(),
                CONFIG_VERSION_CONFLICT,
                "停用配置期望版本与当前版本不一致");
        require(state.version() < Integer.MAX_VALUE,
                CONFIG_VERSION_CONFLICT,
                "配置版本已达到上限");
        requirePersistentId();

        boolean updated = persistence.disableIfVersion(state.id(), expectedVersion);
        require(updated, CONFIG_VERSION_CONFLICT, "停用配置版本竞争失败");
        TypedConfigValue previousValue = state.value();
        state = state.disabled();
        checkInvariants();
        require(Objects.equals(previousValue, state.value()),
                CONFIG_VALUE_INVALID,
                "停用配置不得修改原值");
    }

    public PlatformConfigState state() {
        return state;
    }

    /** 将 uk_biz_id 或 uk_key_scope 冲突转换成稳定的 I1 领域错误。 */
    public static PlatformConfigDomainException identityConflict() {
        return error(CONFIG_IDENTITY_CONFLICT, "配置业务编号或配置身份已存在");
    }

    public static PlatformConfigDomainException identityConflict(Throwable cause) {
        return error(CONFIG_IDENTITY_CONFLICT, "配置业务编号或配置身份已存在", cause);
    }

    /** I1-I4：恢复及每个成功命令后校验全部聚合不变量。 */
    private void checkInvariants() {
        // I1：身份和业务编号非空；不可变值对象与无身份修改入口共同保证建立后不可改。
        requireNotBlank(state.bizId(), CONFIG_IDENTITY_CONFLICT, "配置业务编号不能为空");
        require(state.bizId().length() <= BIZ_ID_MAX,
                CONFIG_IDENTITY_CONFLICT,
                "配置业务编号长度不能超过 32");
        require(state.identity() != null,
                CONFIG_IDENTITY_CONFLICT,
                "配置身份不能为空");

        // I2：当前值的键、类型、内容必须始终通过随应用发布的名录定义。
        require(state.value() != null, CONFIG_VALUE_INVALID, "类型化配置值不能为空");
        validateAgainstDefinition(definition, state.identity(), state.value());

        // I3：值对象只在超过 100000 字符时提前拒绝；
        // 2049-100000 字符与名录固定说明不做表列容量预检。

        // I4：持久化配置从 version=1 起连续递增；更新只在 CAS 成功后复制为 +1 状态。
        require(state.version() >= 1,
                CONFIG_VERSION_CONFLICT,
                "已发布配置版本必须从 1 开始");
        require(state.id() == null || state.id() > 0,
                CONFIG_IDENTITY_CONFLICT,
                "配置数据库主键必须为正数");
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                CONFIG_IDENTITY_CONFLICT,
                "更新配置需要有效数据库主键");
    }

    private static void requirePersistence(PlatformConfigPersistence persistence) {
        require(persistence != null, CONFIG_VERSION_CONFLICT, "配置持久化端口不能为空");
    }

    private static void validateAgainstDefinition(
            PlatformConfigDefinition definition,
            ConfigIdentity identity,
            TypedConfigValue value) {
        require(definition != null, CONFIG_VALUE_INVALID, "配置键不存在于名录");
        require(identity != null, CONFIG_IDENTITY_CONFLICT, "配置身份不能为空");
        require(value != null, CONFIG_VALUE_INVALID, "类型化配置值不能为空");
        try {
            require(Objects.equals(identity.configKey(), definition.configKey()),
                    CONFIG_VALUE_INVALID,
                    "配置键不存在于当前名录定义");
            require(Objects.equals(value.valueType(), definition.valueType()),
                    CONFIG_VALUE_INVALID,
                    "配置值类型与名录类型不一致");
            require(definition.accepts(value.normalizedValue()),
                    CONFIG_VALUE_INVALID,
                    "配置值未通过名录规则校验");
            require(Objects.equals(
                            value.normalizedValue(),
                            definition.normalize(value.normalizedValue())),
                    CONFIG_VALUE_INVALID,
                    "配置值不是名录规则的规范化结果");
        } catch (RuntimeException exception) {
            if (exception instanceof PlatformConfigDomainException domainException) {
                throw domainException;
            }
            throw error(CONFIG_VALUE_INVALID, "配置值未通过名录规则校验", exception);
        }
    }

    private static void requireNotBlank(
            String value, String errorIdentifier, String message) {
        require(value != null && !value.isBlank(), errorIdentifier, message);
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw error(errorIdentifier, message);
        }
    }

    private static PlatformConfigDomainException error(
            String errorIdentifier, String message) {
        return new PlatformConfigDomainException(
                Objects.requireNonNull(errorIdentifier), message);
    }

    private static PlatformConfigDomainException error(
            String errorIdentifier, String message, Throwable cause) {
        return new PlatformConfigDomainException(
                Objects.requireNonNull(errorIdentifier), message, cause);
    }
}
