package com.rally.user.convert;

import com.rally.domain.user.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

@Mapper
public interface ProfileAppConvertMapper {

    ProfileAppConvertMapper INSTANCE = Mappers.getMapper(ProfileAppConvertMapper.class);

    default PlayerHomeVO toPlayerHomeVO(TennisProfileData profileData, UserData userData,
                                         LocalDateTime joinTime) {
        PlayerHomeVO vo = new PlayerHomeVO();
        if (userData != null) {
            vo.setUserId(userData.getUserId());
            vo.setNickname(userData.getNickname());
            vo.setAvatarUrl(userData.getAvatarUrl());
            vo.setGender(userData.getGender() != null ? userData.getGender().name().toLowerCase() : null);
            vo.setBirthday(userData.getBirthday());
            vo.setBio(userData.getBio());
            vo.setCityCode(userData.getCityCode());
        }
        if (profileData != null) {
            vo.setNtrpScore(profileData.getNtrpScore());
            vo.setIsNewbie(profileData.getIsNewbie());
            vo.setIsUnderReview(profileData.getIsUnderReview());
            vo.setVideoUrls(profileData.getVideoUrls());
        }
        vo.setJoinTime(joinTime);
        return vo;
    }
}
