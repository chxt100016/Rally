package com.rally.user.convert;

import com.rally.domain.user.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Mapper
public interface ProfileAppConvertMapper {

    ProfileAppConvertMapper INSTANCE = Mappers.getMapper(ProfileAppConvertMapper.class);

    default MyUserProfileDTO toMyProfileVO(TennisProfileData profileData, UserData userData,
                                          Integer reviewRemainingMatches, Boolean ntrpEditable,
                                          Integer ntrpCooldownRemainingDays) {
        MyUserProfileDTO dto = new MyUserProfileDTO();

        // 设置用户基本信息
        UserVO userVO = new UserVO();
        if (userData != null) {
            userVO.setUserId(userData.getUserId());
            userVO.setNickname(userData.getNickname());
            userVO.setAvatarUrl(userData.getAvatarUrl());
            userVO.setGender(userData.getGender());
            userVO.setBirthday(userData.getBirthday());
        }
        dto.setUser(userVO);

        // 设置网球档案信息
        TennisProfileVO profileVO = new TennisProfileVO();
        if (profileData != null) {
            profileVO.setCityCode(profileData.getCityCode());
            profileVO.setNtrpScore(profileData.getNtrpScore());
            profileVO.setUtrScore(profileData.getUtrScore());
            profileVO.setIsNewbie(profileData.getIsNewbie());
            profileVO.setReputationScore(profileData.getReputationScore());
            profileVO.setCredibilityScore(profileData.getCredibilityScore());
            profileVO.setCalibrationScore(profileData.getCalibrationScore());
            profileVO.setStatus(profileData.getStatus() != null ? profileData.getStatus().name().toLowerCase() : null);
            profileVO.setIsUnderReview(profileData.getIsUnderReview());
            profileVO.setVideoUrls(profileData.getVideoUrls());
            profileVO.setNtrpUpdatedAt(profileData.getNtrpUpdatedAt());
        }
        profileVO.setReviewRemainingMatches(reviewRemainingMatches);
        profileVO.setNtrpEditable(ntrpEditable);
        profileVO.setNtrpCooldownRemainingDays(ntrpCooldownRemainingDays);
        dto.setProfile(profileVO);

        return dto;
    }

    default PlayerHomeVO toPlayerHomeVO(TennisProfileData profileData, UserData userData,
                                         LocalDateTime joinTime) {
        PlayerHomeVO vo = new PlayerHomeVO();
        if (userData != null) {
            vo.setUserId(userData.getUserId());
            vo.setNickname(userData.getNickname());
            vo.setAvatarUrl(userData.getAvatarUrl());
            vo.setGender(userData.getGender() != null ? userData.getGender().name().toLowerCase() : null);
            vo.setBirthday(userData.getBirthday());
        }
        if (profileData != null) {
            vo.setCityCode(profileData.getCityCode());
            vo.setBio(profileData.getBio());
            vo.setNtrpScore(profileData.getNtrpScore());
            vo.setIsNewbie(profileData.getIsNewbie());
            vo.setIsUnderReview(profileData.getIsUnderReview());
            vo.setVideoUrls(profileData.getVideoUrls());
        }
        vo.setJoinTime(joinTime);
        return vo;
    }
}
