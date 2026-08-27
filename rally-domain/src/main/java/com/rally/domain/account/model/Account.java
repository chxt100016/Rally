package com.rally.domain.account.model;

import java.util.Objects;

/**
 * 渠道账户聚合根。
 *
 * <p>一条 {@code account} 记录是加载和保存单位。聚合只有建立命令，账户编号、渠道身份和用户引用
 * 建立后均无改写入口。</p>
 */
public final class Account {

    public static final String IDENTITY_CONFLICT = "ACCOUNT_IDENTITY_CONFLICT";
    public static final String ACCOUNT_ID_IMMUTABLE = "ACCOUNT_ID_IMMUTABLE";
    public static final String BINDING_IMMUTABLE = "ACCOUNT_BINDING_IMMUTABLE";
    public static final String CREDENTIAL_INVALID = "ACCOUNT_CREDENTIAL_INVALID";

    private final AccountData state;

    private Account(AccountData state) {
        this.state = state;
    }

    /**
     * C1 建立渠道账户。
     *
     * <p>唯一性端口做同事务内的前置检查；持久化时还必须依赖
     * {@code uk_channel_identifier} 和 {@code uk_account_id} 抵御并发竞争。</p>
     */
    public static Account bind(
            BindAccountCommand command,
            AccountIdGenerator idGenerator,
            AccountIdentityUniqueness identityUniqueness) {
        require(command != null, BINDING_IMMUTABLE, "建立账户命令不能为空");
        require(idGenerator != null, ACCOUNT_ID_IMMUTABLE, "账户编号生成器不能为空");
        require(identityUniqueness != null, IDENTITY_CONFLICT, "渠道身份唯一性检查器不能为空");

        validateBinding(command.userId(), command.channel(), command.identifier());
        validateCredentials(command.channel(), command.credential());

        AccountIdentity identity = command.identity();
        if (identityUniqueness.exists(identity)) {
            throw identityConflict();
        }

        Account account = new Account(AccountData.newBinding(idGenerator.nextAccountId(), command));
        account.checkInvariants();
        return account;
    }

    /** 从仓储状态恢复聚合；不会装载或校验用户资料。 */
    public static Account restore(AccountData state) {
        require(state != null, BINDING_IMMUTABLE, "账户状态不能为空");
        Account account = new Account(state);
        account.checkInvariants();
        return account;
    }

    /** 聚合的不可变状态快照，交由持久化适配器保存。 */
    public AccountData state() {
        return state;
    }

    public String accountId() {
        return state.accountId();
    }

    public String userId() {
        return state.userId();
    }

    public AccountIdentity identity() {
        return state.identity();
    }

    /**
     * 将数据库唯一键 {@code uk_channel_identifier} 的并发冲突转换为稳定领域错误。
     * 持久化适配器不得把该冲突回查并改判为幂等成功。
     */
    public static AccountDomainException identityConflict() {
        return new AccountDomainException(IDENTITY_CONFLICT, "同一渠道身份已绑定账户");
    }

    public static AccountDomainException identityConflict(Throwable cause) {
        return new AccountDomainException(IDENTITY_CONFLICT, "同一渠道身份已绑定账户", cause);
    }

    /** I1-I4：C1 完成后校验所有与建立命令相关的不变量。 */
    private void checkInvariants() {
        // I1 当前聚合只承载一条渠道身份；跨聚合唯一性由预检与数据库唯一键共同保证。
        require(state.identity() != null, IDENTITY_CONFLICT, "渠道身份不能为空");
        // I2 账户编号非空；不可变状态对象和无更新命令共同保证建立后不得变更。
        requireNotBlank(state.accountId(), ACCOUNT_ID_IMMUTABLE, "账户编号不能为空");
        // I3 渠道、渠道标识与用户引用非空，且不可变状态对象禁止改绑。
        validateBinding(state.userId(), state.channel(), state.identifier());
        // I4 微信小程序账户不得持有本地凭证，unionId 允许为空。
        validateCredentials(state.channel(), state.credential());
    }

    private static void validateBinding(String userId, AccountChannel channel, String identifier) {
        requireNotBlank(userId, BINDING_IMMUTABLE, "用户编号不能为空");
        require(channel != null, BINDING_IMMUTABLE, "渠道不能为空");
        requireNotBlank(identifier, BINDING_IMMUTABLE, "渠道标识不能为空");
    }

    private static void validateCredentials(AccountChannel channel, String credential) {
        if (AccountChannel.WECHAT_MINIAPP.equals(channel)) {
            require(credential == null, CREDENTIAL_INVALID, "微信小程序账户凭证必须为空");
        }
    }

    private static void requireNotBlank(String value, String errorIdentifier, String message) {
        require(value != null && !value.trim().isEmpty(), errorIdentifier, message);
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new AccountDomainException(Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
