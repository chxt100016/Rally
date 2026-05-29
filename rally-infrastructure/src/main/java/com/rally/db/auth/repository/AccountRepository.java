package com.rally.db.auth.repository;

import com.rally.db.auth.entity.AccountPO;
import com.rally.db.auth.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final AccountService accountService;

    public Optional<AccountPO> findByChannelAndIdentifier(String channel, String identifier) {
        return accountService.findByChannelAndIdentifier(channel, identifier);
    }

    public void save(AccountPO account) {
        accountService.save(account);
    }
}
