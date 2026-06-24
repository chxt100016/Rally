package com.rally.db.user.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.TourProfileConvertMapper;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.service.TourProfileService;
import com.rally.db.user.service.UserService;
import com.rally.domain.user.gateway.UserProfileRepository;
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
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserService userService;
    private final TourProfileService tourProfileService;

    @Override
    public UserProfile findByUserId(String userId) {
        UserData userData = userService.findByUserId(userId).map(UserConvertMapper.INSTANCE::toData).orElse(null);
        TourProfileData profileData = tourProfileService.findByUserId(userId).map(TourProfileConvertMapper.INSTANCE::toData).orElse(null);
        return UserProfile.create(userData, profileData);
    }

    @Override
    public List<UserProfile> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        // 批量查询用户和网球档案
        Map<String, UserPO> userMap = listUsers(userIds).stream()
                .collect(Collectors.toMap(UserPO::getUserId, u -> u, (a, b) -> a));
        Map<String, TourProfilePO> profileMap = listProfiles(userIds).stream()
                .collect(Collectors.toMap(TourProfilePO::getUserId, p -> p, (a, b) -> a));
        // 按 userIds 顺序组装 UserProfile
        List<UserProfile> result = new ArrayList<>(userIds.size());
        for (String uid : userIds) {
            UserPO userPO = userMap.get(uid);
            if (userPO == null) {
                result.add(null);
                continue;
            }
            TourProfilePO profilePO = profileMap.get(uid);
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

    private List<UserPO> listUsers(List<String> userIds) {
        return userService.lambdaQuery().in(UserPO::getUserId, userIds).list();
    }

    private List<TourProfilePO> listProfiles(List<String> userIds) {
        return tourProfileService.lambdaQuery().in(TourProfilePO::getUserId, userIds).list();
    }

    /**
     * 保存用户信息，新建或更新
     */
    private void saveUser(UserData userData) {
        if (userData.getUserId() == null) {
            UserPO po = UserConvertMapper.INSTANCE.toPO(userData);
            po.setUserId(IdWorker.getIdStr());
            userService.save(po);
            userData.setUserId(po.getUserId());
        } else {
            userService.findByUserId(userData.getUserId()).ifPresent(po -> {
                UserConvertMapper.INSTANCE.updatePO(po, userData);
                userService.updateById(po);
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
        Optional<TourProfilePO> existing = tourProfileService.findByUserId(profileData.getUserId());
        if (existing.isPresent()) {
            TourProfilePO po = existing.get();
            TourProfileConvertMapper.INSTANCE.updatePO(po, profileData);
            tourProfileService.updateById(po);
        } else {
            TourProfilePO po = TourProfileConvertMapper.INSTANCE.toPO(profileData);
            po.setBizId(IdWorker.getIdStr());
            tourProfileService.save(po);
        }
    }
}
