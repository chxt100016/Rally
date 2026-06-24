package com.rally.domain.auth.gateway;

import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.model.AccountData;

import java.util.Optional;

public interface AccountRepository {
    Optional<AccountData> findByChannelAndIdentifier(ChannelEnum channel, String identifier);
    void createAccount(AccountData account);
}
