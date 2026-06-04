package com.rally.db.user.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.TennisProfileConvertMapper;
import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.repository.TennisProfileRepository;
import com.rally.db.user.repository.UserRepository;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.gateway.UserProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserProfileGatewayImpl implements UserProfileGateway {

    private final UserRepository userRepository;
    private final TennisProfileRepository tennisProfileRepository;
    private static final TennisProfileConvertMapper CONVERTER = TennisProfileConvertMapper.INSTANCE;

    @Override
    public UserProfile findByUserId(String userId) {
        Optional<UserPO> userOpt = userRepository.findByUserId(userId);
        Optional<TennisProfilePO> profileOpt = tennisProfileRepository.findByUserId(userId);

        UserData userData = userOpt.map(this::toUserData).orElse(null);
        TennisProfileData profileData = profileOpt.map(CONVERTER::toData).orElse(null);

        return UserProfile.create(userData, profileData);
    }

    @Override
    public void save(UserProfile userProfile) {
        // 保存 user
        if (userProfile.getUser() != null) {
            saveUser(userProfile.getUser());
        }
        // 保存 profile
        if (userProfile.getProfile() != null) {
            saveProfile(userProfile.getProfile());
        }
    }

    private void saveUser(UserData userData) {
        if (userData.getUserId() == null) {
            // 新建
            UserPO po = new UserPO();
            po.setUserId(IdWorker.getIdStr());
            po.setNickname(userData.getNickname());
            po.setAvatarUrl(userData.getAvatarUrl());
            po.setGender(userData.getGender() != null ? userData.getGender().name().toLowerCase() : GenderEnum.UNDISCLOSED.name().toLowerCase());
            po.setBirthday(userData.getBirthday());
            userRepository.save(po);
            userData.setUserId(po.getUserId());
        } else {
            // 更新
            Optional<UserPO> existing = userRepository.findByUserId(userData.getUserId());
            if (existing.isPresent()) {
                UserPO po = existing.get();
                if (userData.getNickname() != null) po.setNickname(userData.getNickname());
                if (userData.getAvatarUrl() != null) po.setAvatarUrl(userData.getAvatarUrl());
                if (userData.getGender() != null) po.setGender(userData.getGender().name().toLowerCase());
                if (userData.getBirthday() != null) po.setBirthday(userData.getBirthday());
                userRepository.updateById(po);
            }
        }
    }

    private void saveProfile(TennisProfileData profileData) {
        if (profileData.getUserId() == null) {
            return;
        }
        Optional<TennisProfilePO> existing = tennisProfileRepository.findByUserId(profileData.getUserId());
        if (existing.isPresent()) {
            // 更新
            TennisProfilePO po = existing.get();
            if (profileData.getCityCode() != null) po.setCityCode(profileData.getCityCode());
            if (profileData.getBio() != null) po.setBio(profileData.getBio());
            if (profileData.getVideoUrls() != null) po.setVideoUrls(CONVERTER.stringListToJson(profileData.getVideoUrls()));
            if (profileData.getNtrpScore() != null) po.setNtrpScore(profileData.getNtrpScore());
            if (profileData.getNtrpUpdatedAt() != null) po.setNtrpUpdatedAt(profileData.getNtrpUpdatedAt());
            if (profileData.getStatus() != null) po.setStatus(CONVERTER.profileStatusToString(profileData.getStatus()));
            if (profileData.getIsUnderReview() != null) po.setIsUnderReview(profileData.getIsUnderReview());
            tennisProfileRepository.updateById(po);
        } else {
            // 新建
            TennisProfilePO po = CONVERTER.toPO(profileData);
            po.setBizId(IdWorker.getIdStr());
            tennisProfileRepository.save(po);
        }
    }

    private UserData toUserData(UserPO po) {
        UserData data = new UserData();
        data.setUserId(po.getUserId());
        data.setNickname(po.getNickname());
        data.setAvatarUrl(po.getAvatarUrl());
        data.setBirthday(po.getBirthday());
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
