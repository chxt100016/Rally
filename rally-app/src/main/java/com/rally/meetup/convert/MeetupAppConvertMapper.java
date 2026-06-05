package com.rally.meetup.convert;

import com.rally.domain.meetup.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 约球域 MapStruct 转换器
 */
@Mapper
public interface MeetupAppConvertMapper {

    MeetupAppConvertMapper INSTANCE = Mappers.getMapper(MeetupAppConvertMapper.class);

    // ==================== MeetupData → MeetupVO ====================

    @Mapping(target = "meetupId", source = "bizId")
    @Mapping(target = "perPersonCost", ignore = true)
    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "actionState", ignore = true)
    @Mapping(target = "quitWillPenalize", ignore = true)
    @Mapping(target = "creatorNickname", ignore = true)
    @Mapping(target = "creatorAvatarUrl", ignore = true)
    @Mapping(target = "creatorNtrp", ignore = true)
    @Mapping(target = "participants", ignore = true)
    MeetupVO toMeetupVO(MeetupData data);

    List<MeetupVO> toMeetupVOList(List<MeetupData> dataList);

    // ==================== MeetupData → MeetupCardVO ====================

    @Mapping(target = "meetupId", source = "bizId")
    @Mapping(target = "perPersonCost", ignore = true)
    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "actionState", ignore = true)
    @Mapping(target = "creatorNickname", ignore = true)
    @Mapping(target = "creatorAvatarUrl", ignore = true)
    MeetupCardVO toMeetupCardVO(MeetupData data);

    List<MeetupCardVO> toMeetupCardVOList(List<MeetupData> dataList);

    // ==================== RegistrationData → RegistrationVO ====================

    @Mapping(target = "registrationId", source = "bizId")
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "ntrpScore", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    RegistrationVO toRegistrationVO(RegistrationData data);

    List<RegistrationVO> toRegistrationVOList(List<RegistrationData> dataList);
}
