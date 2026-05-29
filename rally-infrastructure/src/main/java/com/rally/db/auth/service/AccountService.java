package com.rally.db.auth.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.auth.entity.AccountPO;
import com.rally.db.auth.mapper.AccountMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService extends ServiceImpl<AccountMapper, AccountPO> {

    public Optional<AccountPO> findByChannelAndIdentifier(String channel, String identifier) {
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(AccountPO::getChannel, channel)
                        .eq(AccountPO::getIdentifier, identifier)
                        .last("LIMIT 1")
                        .one()
        );
    }
}
