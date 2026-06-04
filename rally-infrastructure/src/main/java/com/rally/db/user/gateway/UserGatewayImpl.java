package com.rally.db.user.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.repository.UserRepository;
import com.rally.domain.user.enums.GenderEnum;
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
        UserPO po = new UserPO();
        po.setUserId(IdWorker.getIdStr());
        po.setNickname(user.getNickname());
        po.setAvatarUrl(user.getAvatarUrl());
        po.setGender(user.getGender() != null ? user.getGender().name().toLowerCase() : GenderEnum.UNDISCLOSED.name().toLowerCase());
        po.setBirthday(user.getBirthday());
        po.setBio(user.getBio());
        userRepository.save(po);

        user.setUserId(po.getUserId());
        return user;
    }

    @Override
    public Optional<UserData> findByUserId(String userId) {
        return userRepository.findByUserId(userId).map(this::toData);
    }

    @Override
    public UserData updateUser(UserData user) {
        UserPO po = userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getNickname() != null) {
            po.setNickname(user.getNickname());
        }
        if (user.getAvatarUrl() != null) {
            po.setAvatarUrl(user.getAvatarUrl());
        }
        if (user.getGender() != null) {
            po.setGender(user.getGender().name().toLowerCase());
        }
        if (user.getBirthday() != null) {
            po.setBirthday(user.getBirthday());
        }
        if (user.getBio() != null) {
            po.setBio(user.getBio());
        }
        userRepository.updateById(po);

        return toData(po);
    }

    private UserData toData(UserPO po) {
        UserData data = new UserData();
        data.setUserId(po.getUserId());
        data.setNickname(po.getNickname());
        data.setAvatarUrl(po.getAvatarUrl());
        data.setBirthday(po.getBirthday());
        data.setBio(po.getBio());
        if (po.getGender() != null) {
            try {
                data.setGender(GenderEnum.valueOf(po.getGender().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                data.setGender(GenderEnum.UNDISCLOSED);
            }
        }
        return data;
    }
}
