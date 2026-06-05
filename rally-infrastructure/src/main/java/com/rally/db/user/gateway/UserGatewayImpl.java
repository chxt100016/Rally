package com.rally.db.user.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.repository.UserRepository;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserGatewayImpl implements UserGateway {

    private final UserRepository userRepository;

    @Override
    public UserData createUser(UserData user) {
        UserPO po = UserConvertMapper.INSTANCE.toPO(user);
        po.setUserId(IdWorker.getIdStr());
        userRepository.save(po);

        user.setUserId(po.getUserId());
        return user;
    }

    @Override
    public Optional<UserData> findByUserId(String userId) {
        return userRepository.findByUserId(userId).map(this::toData);
    }

    @Override
    public void updateUser(UserData user) {
        UserPO exist = userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserConvertMapper.INSTANCE.updatePO(exist, user);
        userRepository.updateById(exist);

    }

    private UserData toData(UserPO po) {
        return UserConvertMapper.INSTANCE.toData(po);
    }
}
