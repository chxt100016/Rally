package com.rally.db.user.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.service.UserService;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserService userService;
    private static final UserConvertMapper MAPPER = UserConvertMapper.INSTANCE;

    @Override
    public UserData createUser(UserData user) {
        UserPO po = MAPPER.toPO(user);
        po.setUserId(IdWorker.getIdStr());
        userService.save(po);
        user.setUserId(po.getUserId());
        return user;
    }

    @Override
    public Optional<UserData> findByUserId(String userId) {
        return userService.findByUserId(userId).map(MAPPER::toData);
    }

    @Override
    public void updateUser(UserData user) {
        UserPO exist = userService.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        MAPPER.updatePO(exist, user);
        userService.updateById(exist);
    }
}
