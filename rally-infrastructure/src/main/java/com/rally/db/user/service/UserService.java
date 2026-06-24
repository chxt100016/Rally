package com.rally.db.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService extends ServiceImpl<UserMapper, UserPO> {

    public Optional<UserPO> findByUserId(String userId) {
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(UserPO::getUserId, userId)
                        .last("LIMIT 1")
                        .one()
        );
    }
}
