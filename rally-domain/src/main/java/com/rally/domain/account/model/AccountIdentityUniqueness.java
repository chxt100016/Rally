package com.rally.domain.account.model;

/**
 * 渠道身份唯一性预检端口。
 *
 * <p>它只能用于读取；并发写入仍由 account.uk_channel_identifier 最终裁决。</p>
 */
@FunctionalInterface
public interface AccountIdentityUniqueness {

    boolean exists(AccountIdentity identity);
}
