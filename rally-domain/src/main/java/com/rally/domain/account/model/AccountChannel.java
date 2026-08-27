package com.rally.domain.account.model;

/**
 * 渠道账户支持的身份渠道。
 *
 * <p>领域层使用语义化枚举；持久化适配器负责映射为表中的小写值。</p>
 */
public enum AccountChannel {
    PHONE("phone"),
    WECHAT_MINIAPP("wechat_miniapp");

    private final String persistenceValue;

    AccountChannel(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String persistenceValue() {
        return persistenceValue;
    }

    public static AccountChannel fromPersistenceValue(String value) {
        for (AccountChannel channel : values()) {
            if (channel.persistenceValue.equals(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unsupported account channel: " + value);
    }
}
