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

    default TennisProfileVO toMyProfileVO(TennisProfileData profileData, UserData userData,
                                          Integer reviewRemainingMatches, Boolean ntrpEditable,
                                          Integer ntrpCooldownRemainingDays) {
        TennisProfileVO vo = new TennisProfileVO();
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
            vo.setUtrScore(profileData.getUtrScore());
            vo.setRatingLevel(profileData.getRatingLevel() != null ? profileData.getRatingLevel().name() : null);
            vo.setIsNewbie(profileData.getIsNewbie());
            vo.setReputationScore(profileData.getReputationScore());
            vo.setCredibilityScore(profileData.getCredibilityScore());
            vo.setCalibrationScore(profileData.getCalibrationScore());
            vo.setTotalScore(profileData.getTotalScore());
            vo.setStatus(profileData.getStatus() != null ? profileData.getStatus().name().toLowerCase() : null);
            vo.setIsUnderReview(profileData.getIsUnderReview());
            vo.setVideoUrls(profileData.getVideoUrls());
            vo.setNtrpUpdatedAt(profileData.getNtrpUpdatedAt());
        }
        vo.setReviewRemainingMatches(reviewRemainingMatches);
        vo.setNtrpEditable(ntrpEditable);
        vo.setNtrpCooldownRemainingDays(ntrpCooldownRemainingDays);
        return vo;
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
            vo.setRatingLevel(profileData.getRatingLevel() != null ? profileData.getRatingLevel().name() : null);
            vo.setIsNewbie(profileData.getIsNewbie());
            vo.setIsUnderReview(profileData.getIsUnderReview());
            vo.setVideoUrls(profileData.getVideoUrls());
        }
        vo.setJoinTime(joinTime);
        return vo;
    }
}
