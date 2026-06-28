package com.rally.domain.auth.gateway;

import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.model.AccountData;

import java.util.Optional;

public interface AccountRepository {
    Optional<AccountData> findByChannelAndIdentifier(ChannelEnum channel, String identifier);
    void createAccount(AccountData account);

    /** 反查用户在指定渠道下的 identifier（微信小程序即 openid），无则返回 null。 */
    String findIdentifierByUser(String userId, ChannelEnum channel);
}
