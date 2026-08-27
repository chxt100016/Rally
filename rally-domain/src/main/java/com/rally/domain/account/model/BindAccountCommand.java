package com.rally.domain.account.model;

/** C1 建立渠道账户的命令入参。 */
public record BindAccountCommand(
        String userId,
        AccountChannel channel,
        String identifier,
        String credential,
        String unionId) {

    public AccountIdentity identity() {
        return new AccountIdentity(channel, identifier);
    }

    public AccountCredentials credentials() {
        return new AccountCredentials(credential, unionId);
    }
}
