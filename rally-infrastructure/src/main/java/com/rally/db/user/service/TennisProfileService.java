package com.rally.db.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.mapper.TennisProfileMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TennisProfileService extends ServiceImpl<TennisProfileMapper, TennisProfilePO> {

    public TennisProfilePO insert(TennisProfilePO profile) {
        this.save(profile);
        return profile;
    }

    public Optional<TennisProfilePO> findByUserId(String userId) {
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(TennisProfilePO::getUserId, userId)
                        .last("LIMIT 1")
                        .one()
        );
    }
}
