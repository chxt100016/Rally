package com.rally.db.user.convert;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.enums.RatingLevelEnum;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.db.user.entity.TennisProfilePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TennisProfileConvertMapper {

    TennisProfileConvertMapper INSTANCE = Mappers.getMapper(TennisProfileConvertMapper.class);

    @Named("toData")
    @Mapping(target = "videoUrls", source = "videoUrls", qualifiedByName = "jsonToStringList")
    TennisProfileData toData(TennisProfilePO po);

    @Named("toPO")
    @Mapping(target = "videoUrls", source = "videoUrls", qualifiedByName = "stringListToJson")
    TennisProfilePO toPO(TennisProfileData data);

    @Named("stringToProfileStatus")
    default ProfileStatusEnum stringToProfileStatus(String status) {
        if (status == null) {
            return ProfileStatusEnum.TBC;
        }
        try {
            return ProfileStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProfileStatusEnum.TBC;
        }
    }

    @Named("profileStatusToString")
    default String profileStatusToString(ProfileStatusEnum status) {
        if (status == null) {
            return "tbc";
        }
        return status.name().toLowerCase();
    }

    @Named("stringToRatingLevel")
    default RatingLevelEnum stringToRatingLevel(String level) {
        if (level == null) {
            return RatingLevelEnum.A;
        }
        try {
            return RatingLevelEnum.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RatingLevelEnum.A;
        }
    }

    @Named("ratingLevelToString")
    default String ratingLevelToString(RatingLevelEnum level) {
        if (level == null) {
            return "a";
        }
        return level.name().toLowerCase();
    }

    @Named("jsonToStringList")
    default List<String> jsonToStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("stringListToJson")
    default String stringListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }
}
