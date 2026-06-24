package com.rally.db.auth.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.auth.entity.AccountPO;
import com.rally.db.auth.service.AccountService;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.auth.model.AccountData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountService accountService;

    @Override
    public Optional<AccountData> findByChannelAndIdentifier(ChannelEnum channel, String identifier) {
        return accountService.findByChannelAndIdentifier(channel.name().toLowerCase(), identifier).map(this::toData);
    }

    @Override
    public void createAccount(AccountData account) {
        AccountPO po = new AccountPO();
        po.setAccountId(IdWorker.getIdStr());
        po.setUserId(account.getUserId());
        po.setChannel(account.getChannel().name().toLowerCase());
        po.setIdentifier(account.getIdentifier());
        po.setUnionId(account.getUnionId());
        accountService.save(po);
    }

    private AccountData toData(AccountPO po) {
        AccountData data = new AccountData();
        data.setAccountId(po.getAccountId());
        data.setUserId(po.getUserId());
        data.setIdentifier(po.getIdentifier());
        data.setUnionId(po.getUnionId());
        if (po.getChannel() != null) {
            try {
                data.setChannel(ChannelEnum.valueOf(po.getChannel().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }
}
