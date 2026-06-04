package com.rally.user.convert;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.user.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ProfileAppConvertMapper {

    ProfileAppConvertMapper INSTANCE = Mappers.getMapper(ProfileAppConvertMapper.class);

    /**
     * 转换为我的档案DTO（新版）
     */
    default MyProfileDTO toMyProfileDTO(TennisProfileData profileData, UserData userData,
                                         Integer reviewTotal, String scoreLevel,
                                         List<ScoreItemDTO> scoreItems,
                                         Integer lockday, Integer remainingMatches) {
        MyProfileDTO dto = new MyProfileDTO();

        // 设置约球信息（暂时默认99）
        MyProfileMeetupDTO meetupDTO = new MyProfileMeetupDTO();
        meetupDTO.setCompletedCount(99);
        dto.setMeetup(meetupDTO);

        // 设置评价信息（暂时默认99，标签默认"可爱"、"发球好"）
        MyProfileReviewDTO reviewDTO = new MyProfileReviewDTO();
        reviewDTO.setTotal(reviewTotal != null ? reviewTotal : 99);
        List<ReviewTagDTO> tags = new ArrayList<>();
        ReviewTagDTO tag1 = new ReviewTagDTO();
        tag1.setName("可爱");
        tags.add(tag1);
        ReviewTagDTO tag2 = new ReviewTagDTO();
        tag2.setName("发球好");
        tags.add(tag2);
        reviewDTO.setTags(tags);
        dto.setReview(reviewDTO);

        // 设置等级信息
        MyProfileLevelDTO levelDTO = new MyProfileLevelDTO();
        if (profileData != null) {
            levelDTO.setNtrpScore(profileData.getNtrpScore());
            levelDTO.setIsUnderReview(profileData.getIsUnderReview());
        }
        levelDTO.setLockday(lockday);
        levelDTO.setRemainingMatches(remainingMatches);
        // 系统建议暂时为空，后续完善
        LevelSuggestionDTO suggestionDTO = new LevelSuggestionDTO();
        levelDTO.setSuggestion(suggestionDTO);
        dto.setLevel(levelDTO);

        // 设置评分信息
        MyProfileScoreDTO scoreDTO = new MyProfileScoreDTO();
        scoreDTO.setScoreLevel(scoreLevel);
        scoreDTO.setData(scoreItems != null ? scoreItems : new ArrayList<>());
        dto.setScore(scoreDTO);

        // 设置用户基本信息
        MyProfileUserDTO userDTO = new MyProfileUserDTO();
        if (userData != null) {
            userDTO.setUserId(userData.getUserId());
            userDTO.setNickname(userData.getNickname());
            userDTO.setAvatarUrl(userData.getAvatarUrl());
            userDTO.setGender(userData.getGender());
            userDTO.setBirthday(userData.getBirthday());
            userDTO.setCityCode(userData.getCityCode());
            userDTO.setBio(userData.getBio());
        }
        dto.setUser(userDTO);

        // 设置视频信息
        MyProfileVideoDTO videoDTO = new MyProfileVideoDTO();
        if (profileData != null && profileData.getVideoUrls() != null) {
            List<String> videoKeys = profileData.getVideoUrls();
            videoDTO.setTotal(videoKeys.size());
            List<VideoItemDTO> videoItems = new ArrayList<>();
            for (String key : videoKeys) {
                VideoItemDTO item = new VideoItemDTO();
                item.setKey(key);
                item.setUrl(QiniuConfiguration.buildSignedUrl(key));
                videoItems.add(item);
            }
            videoDTO.setData(videoItems);
        } else {
            videoDTO.setTotal(0);
            videoDTO.setData(new ArrayList<>());
        }
        dto.setVideo(videoDTO);

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
