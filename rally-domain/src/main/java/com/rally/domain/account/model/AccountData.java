package com.rally.domain.account.model;

import java.time.LocalDateTime;

/**
 * 渠道账户的不可变状态快照。
 *
 * <p>技术主键和时间由持久化层承载；业务状态没有更新命令，因此所有业务字段建立后均不可改写。</p>
 */
public record AccountData(
        Long id,
        String accountId,
        String userId,
        AccountChannel channel,
        String identifier,
        String credential,
        String unionId,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static AccountData newBinding(String accountId, BindAccountCommand command) {
        return new AccountData(
                null,
                accountId,
                command.userId(),
                command.channel(),
                command.identifier(),
                command.credential(),
                command.unionId(),
                null,
                null);
    }

    public AccountIdentity identity() {
        return new AccountIdentity(channel, identifier);
    }

    public AccountCredentials credentials() {
        return new AccountCredentials(credential, unionId);
    }
}
