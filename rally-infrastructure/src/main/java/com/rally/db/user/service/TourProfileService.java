package com.rally.db.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.mapper.TourProfileMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TourProfileService extends ServiceImpl<TourProfileMapper, TourProfilePO> {

    public Optional<TourProfilePO> findByUserId(String userId) {
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(TourProfilePO::getUserId, userId)
                        .last("LIMIT 1")
                        .one()
        );
    }
}
