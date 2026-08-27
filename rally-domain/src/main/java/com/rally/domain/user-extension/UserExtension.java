package com.rally.domain.identity.userextension;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.UserExtKeyEnum;

import java.util.Arrays;
import java.util.Objects;

/**
 * 用户扩展资料聚合根。
 *
 * <p>一条 {@code user_ext} 记录是加载和保存单位。业务写入只能通过 C1/C2；
 * 基础用户与扩展值指向的外部资源不属于本聚合。</p>
 */
public final class UserExtension {

    public static final String WECHAT_PAYMENT_CODE = "wechat_payment_code";

    private UserExtensionState state;
    private final UserExtensionIdentity identity;
    private UserExtensionStatus status;

    private UserExtension(UserExtensionState state, UserExtensionStatus status) {
        this.state = state;
        this.identity = UserExtensionIdentity.of(state.userId(), state.extensionKey());
        this.status = status;
    }

    /** 从仓储返回的完整业务编号和值快照恢复 PRESENT 聚合。 */
    public static UserExtension restore(UserExtensionState state) {
        if (state == null) {
            throw new BusinessException(BizErrorCode.USER_EXT_NOT_FOUND);
        }
        UserExtension extension = new UserExtension(state, UserExtensionStatus.PRESENT);
        extension.checkInvariants();
        return extension;
    }

    /**
     * C1：首次保存或覆盖扩展资料。
     *
     * <p>每次调用都先生成新业务编号。已存在时仅沿用数据库自增主键，
     * 业务编号与扩展值一起被覆盖，时间字段由数据库处理。</p>
     */
    public static UserExtension save(SaveUserExtensionCommand command,
                                     UserExtensionState currentState,
                                     UserExtensionIdGenerator idGenerator) {
        if (command == null) {
            throw keyConflict("保存命令不能为空");
        }
        UserExtensionIdentity requestedIdentity = command.identity();
        validateExtensionKey(requestedIdentity.getExtensionKey());
        validateContent(requestedIdentity.getExtensionKey(), command.getExtensionValue());

        Objects.requireNonNull(idGenerator, "业务编号生成器不能为空");
        String businessId = idGenerator.nextBusinessId();
        if (isBlank(businessId)) {
            throw new IllegalStateException("业务编号生成器返回了空值");
        }

        Long persistedId = null;
        if (currentState != null) {
            UserExtensionIdentity currentIdentity =
                    UserExtensionIdentity.of(currentState.userId(), currentState.extensionKey());
            if (!currentIdentity.equals(requestedIdentity)) {
                throw keyConflict("保存命令与当前扩展身份不一致");
            }
            persistedId = currentState.id();
        }

        UserExtension extension = new UserExtension(
                UserExtensionState.forSave(persistedId, businessId, command),
                UserExtensionStatus.PRESENT);
        extension.checkInvariants();
        return extension;
    }

    /**
     * C2：二次读取证明资料存在后，准备按用户和扩展键删除。
     *
     * <p>不比较业务编号或值；持久化层只使用 {@link #identity()}
     * 作为删除条件，并且不检查影响行数。</p>
     */
    public static UserExtension remove(RemoveUserExtensionCommand command,
                                       UserExtensionState secondReadState) {
        Objects.requireNonNull(command, "删除命令不能为空");
        UserExtensionIdentity requestedIdentity = command.identity();
        validateExtensionKey(requestedIdentity.getExtensionKey());
        if (secondReadState == null) {
            throw new BusinessException(BizErrorCode.USER_EXT_NOT_FOUND);
        }

        UserExtension extension = restore(secondReadState);
        if (!extension.identity.equals(requestedIdentity)) {
            throw keyConflict("二次读取的扩展身份与删除命令不一致");
        }

        extension.status = UserExtensionStatus.REMOVED;
        extension.checkInvariants();
        return extension;
    }

    /** 将数据库组合唯一键冲突转换为 I1 的稳定领域错误。 */
    public static UserExtensionDomainException keyConflict(Throwable cause) {
        return new UserExtensionDomainException(
                UserExtensionError.USER_EXTENSION_KEY_CONFLICT,
                "同一用户与扩展键最多只能存在一条记录",
                cause);
    }

    public UserExtensionState state() {
        return state;
    }

    public UserExtensionIdentity identity() {
        return identity;
    }

    public UserExtensionStatus status() {
        return status;
    }

    /** I1-I4：每个命令完成后校验与当前状态相关的全部聚合不变量。 */
    private void checkInvariants() {
        UserExtensionIdentity currentIdentity =
                UserExtensionIdentity.of(state.userId(), state.extensionKey());
        if (!identity.equals(currentIdentity)) {
            throw keyConflict("用户编号或扩展键不得在聚合内改变");
        }

        if (isBlank(state.businessId())) {
            throw new IllegalStateException("持久化的业务编号不能为空");
        }

        validateExtensionKey(state.extensionKey());
        validateContent(state.extensionKey(), state.extensionValue());
    }

    private static void validateExtensionKey(String extensionKey) {
        boolean valid = Arrays.stream(UserExtKeyEnum.values())
                .anyMatch(candidate -> candidate.getKey().equals(extensionKey));
        if (!valid) {
            throw new BusinessException(BizErrorCode.USER_EXT_KEY_INVALID);
        }
    }

    private static void validateContent(String extensionKey, String extensionValue) {
        if (WECHAT_PAYMENT_CODE.equals(extensionKey) && isBlank(extensionValue)) {
            throw new UserExtensionDomainException(
                    UserExtensionError.PAYMENT_CODE_EMPTY,
                    "收款码扩展值不能为空");
        }
    }

    private static UserExtensionDomainException keyConflict(String message) {
        return new UserExtensionDomainException(
                UserExtensionError.USER_EXTENSION_KEY_CONFLICT,
                message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
