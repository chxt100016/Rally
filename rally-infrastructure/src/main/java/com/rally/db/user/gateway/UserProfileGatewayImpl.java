package com.rally.db.user.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.TourProfileConvertMapper;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.repository.TourProfileRepository;
import com.rally.db.user.repository.UserRepository;
import com.rally.domain.user.gateway.UserProfileGateway;
import com.rally.domain.user.model.TourProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserProfileGatewayImpl implements UserProfileGateway {

    private final UserRepository userRepository;
    private final TourProfileRepository tourProfileRepository;

    @Override
    public UserProfile findByUserId(String userId) {
        Optional<UserPO> userOpt = userRepository.findByUserId(userId);
        Optional<TourProfilePO> profileOpt = tourProfileRepository.findByUserId(userId);

        UserData userData = userOpt.map(UserConvertMapper.INSTANCE::toData).orElse(null);
        TourProfileData profileData = profileOpt.map(TourProfileConvertMapper.INSTANCE::toData).orElse(null);

        return UserProfile.create(userData, profileData);
    }

    @Override
    public List<UserProfile> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        // 批量查询用户和网球档案
        Map<String, UserPO> userMap = userRepository.findByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserPO::getUserId, u -> u, (a, b) -> a));
        Map<String, TourProfilePO> profileMap = tourProfileRepository.findByUserIds(userIds).stream()
                .collect(Collectors.toMap(TourProfilePO::getUserId, p -> p, (a, b) -> a));
        // 按 userIds 顺序组装 UserProfile
        List<UserProfile> result = new ArrayList<>(userIds.size());
        for (String uid : userIds) {
            UserPO userPO = userMap.get(uid);
            TourProfilePO profilePO = profileMap.get(uid);
            if (userPO == null) {
                result.add(null);
                continue;
            }
            UserData userData = UserConvertMapper.INSTANCE.toData(userPO);
            TourProfileData profileData = profilePO != null ? TourProfileConvertMapper.INSTANCE.toData(profilePO) : null;
            result.add(UserProfile.create(userData, profileData));
        }
        return result;
    }

    @Override
    public void save(UserProfile userProfile) {
        if (userProfile.getUser() != null) {
            saveUser(userProfile.getUser());
        }
        if (userProfile.getProfile() != null) {
            saveProfile(userProfile.getProfile());
        }
    }

    /**
     * 保存用户信息，新建或更新
     */
    private void saveUser(UserData userData) {
        if (userData.getUserId() == null) {
            // 新建用户
            UserPO po = UserConvertMapper.INSTANCE.toPO(userData);
            po.setUserId(IdWorker.getIdStr());
            userRepository.save(po);
            // 回写生成的 userId
            userData.setUserId(po.getUserId());
        } else {
            // 更新用户，只更新非空字段
            userRepository.findByUserId(userData.getUserId()).ifPresent(po -> {
                UserConvertMapper.INSTANCE.updatePO(po, userData);
                userRepository.updateById(po);
            });
        }
    }

    /**
     * 保存网球档案，新建或更新
     */
    private void saveProfile(TourProfileData profileData) {
        if (profileData.getUserId() == null) {
            return;
        }
        Optional<TourProfilePO> existing = tourProfileRepository.findByUserId(profileData.getUserId());
        if (existing.isPresent()) {
            // 更新档案，只更新非空字段
            TourProfilePO po = existing.get();
            TourProfileConvertMapper.INSTANCE.updatePO(po, profileData);
            tourProfileRepository.updateById(po);
        } else {
            // 新建档案
            TourProfilePO po = TourProfileConvertMapper.INSTANCE.toPO(profileData);
            po.setBizId(IdWorker.getIdStr());
            tourProfileRepository.save(po);
        }
    }
}
