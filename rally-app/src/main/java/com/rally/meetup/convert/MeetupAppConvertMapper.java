package com.rally.meetup.convert;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.model.*;
import com.rally.domain.recap.model.RecapDTO;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ScoreRecordData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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
    @Mapping(target = "recap", ignore = true)
    MeetupVO toMeetupVO(MeetupData data);

    List<MeetupVO> toMeetupVOList(List<MeetupData> dataList);

    // ==================== MeetupData → MeetupCardDTO ====================

    @Mapping(target = "meetupId", source = "bizId")
    @Mapping(target = "primaryLabel", ignore = true)
    MeetupCardDTO toMeetupCardDTO(MeetupData data);

    List<MeetupCardDTO> toMeetupCardDTOList(List<MeetupData> dataList);




    // ==================== MeetupData → MeetupDTO ====================

    @Mapping(target = "meetupId", source = "bizId")
    @Mapping(target = "matchTypeLabel", expression = "java(getMatchTypeLabel(data))")
    MeetupDTO toMeetupDTO(MeetupData data);

    default String getMatchTypeLabel(MeetupData data) {
        return data.getMatchType().getName();
    }

    // ==================== RegistrationData → RegistrationVO ====================

    @Mapping(target = "registrationId", source = "bizId")
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "ntrpScore", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    RegistrationVO toRegistrationVO(RegistrationData data);

    List<RegistrationVO> toRegistrationVOList(List<RegistrationData> dataList);

    // ==================== ReviewData → RecapDTO.ReviewItem ====================

    @Mapping(target = "type", source = "reviewType", qualifiedByName = "reviewTypeToStr")
    @Mapping(target = "value", source = "reviewValue")
    RecapDTO.ReviewItem toReviewItem(ReviewData data);

    List<RecapDTO.ReviewItem> toReviewItemList(List<ReviewData> dataList);

    // ==================== ScoreRecordData → RecapDTO.ScoreItem ====================

    @Mapping(target = "setNum", source = "setNumber")
    @Mapping(target = "scoreVersion", source = "version")
    @Mapping(target = "setFormat", source = "setFormat", qualifiedByName = "setFormatToStr")
    @Mapping(target = "sideAPlayer1Avatar", source = "sideAPlayer1Avatar", qualifiedByName = "parseAvatar")
    @Mapping(target = "sideAPlayer2Avatar", source = "sideAPlayer2Avatar", qualifiedByName = "parseAvatar")
    @Mapping(target = "sideBPlayer1Avatar", source = "sideBPlayer1Avatar", qualifiedByName = "parseAvatar")
    @Mapping(target = "sideBPlayer2Avatar", source = "sideBPlayer2Avatar", qualifiedByName = "parseAvatar")
    RecapDTO.ScoreItem toScoreItem(ScoreRecordData data);

    List<RecapDTO.ScoreItem> toScoreItemList(List<ScoreRecordData> dataList);

    // ==================== 枚举转换 ====================

    @Named("parseAvatar")
    static String parseAvatar(String key) {
        return QiniuConfiguration.buildSignedUrl(key);
    }

    @Named("reviewTypeToStr")
    static String reviewTypeToStr(com.rally.domain.recap.enums.ReviewTypeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("setFormatToStr")
    static String setFormatToStr(com.rally.domain.recap.enums.SetFormatEnum value) {
        return value == null ? null : value.name();
    }
}
