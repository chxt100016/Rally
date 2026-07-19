package com.rally.db.meetup.convert;

import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.meetup.model.CostData;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MeetupConvertMapper {

    MeetupConvertMapper INSTANCE = Mappers.getMapper(MeetupConvertMapper.class);

    // ==================== MeetupPO ↔ MeetupData ====================

    @Mapping(target = "matchType", source = "matchType", qualifiedByName = "strToMatchType")
    @Mapping(target = "levelMode", source = "levelMode", qualifiedByName = "strToLevelMode")
    @Mapping(target = "genderLimit", source = "genderLimit", qualifiedByName = "strToGenderLimit")
    @Mapping(target = "joinMode", source = "joinMode", qualifiedByName = "strToJoinMode")
    @Mapping(target = "status", source = "status", qualifiedByName = "strToMeetupStatus")
    @Mapping(target = "courtSelectMode", source = "courtSelectMode", qualifiedByName = "strToCourtSelectMode")
    @Mapping(target = "costData", source = "costData", qualifiedByName = "jsonToCostData")
    @Mapping(target = "pendingReason", source = "pendingReason", qualifiedByName = "strToPendingReason")
    MeetupData toMeetupData(MeetupPO po);

    List<MeetupData> toMeetupDataList(List<MeetupPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "matchType", source = "matchType", qualifiedByName = "matchTypeToStr")
    @Mapping(target = "levelMode", source = "levelMode", qualifiedByName = "levelModeToStr")
    @Mapping(target = "genderLimit", source = "genderLimit", qualifiedByName = "genderLimitToStr")
    @Mapping(target = "joinMode", source = "joinMode", qualifiedByName = "joinModeToStr")
    @Mapping(target = "status", source = "status", qualifiedByName = "meetupStatusToStr")
    @Mapping(target = "courtSelectMode", source = "courtSelectMode", qualifiedByName = "courtSelectModeToStr")
    @Mapping(target = "costData", source = "costData", qualifiedByName = "costDataToJson")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "pendingReason", ignore = true)
    MeetupPO toMeetupPO(MeetupData data);

    // ==================== 枚举转换 ====================

    @Named("strToMatchType")
    static MatchTypeEnum strToMatchType(String value) {
        return value == null ? null : MatchTypeEnum.valueOf(value);
    }

    @Named("matchTypeToStr")
    static String matchTypeToStr(MatchTypeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToLevelMode")
    static LevelModeEnum strToLevelMode(String value) {
        return value == null ? null : LevelModeEnum.valueOf(value);
    }

    @Named("levelModeToStr")
    static String levelModeToStr(LevelModeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToGenderLimit")
    static GenderLimitEnum strToGenderLimit(String value) {
        return value == null ? null : GenderLimitEnum.valueOf(value);
    }

    @Named("genderLimitToStr")
    static String genderLimitToStr(GenderLimitEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToJoinMode")
    static JoinModeEnum strToJoinMode(String value) {
        return value == null ? null : JoinModeEnum.valueOf(value);
    }

    @Named("joinModeToStr")
    static String joinModeToStr(JoinModeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToMeetupStatus")
    static MeetupStatusEnum strToMeetupStatus(String value) {
        return value == null ? null : MeetupStatusEnum.valueOf(value);
    }

    @Named("meetupStatusToStr")
    static String meetupStatusToStr(MeetupStatusEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToPendingReason")
    static PendingReasonEnum strToPendingReason(String value) {
        return value == null ? null : PendingReasonEnum.valueOf(value);
    }

    @Named("strToCourtSelectMode")
    static CourtSelectModeEnum strToCourtSelectMode(String value) {
        return value == null ? null : CourtSelectModeEnum.valueOf(value);
    }

    @Named("courtSelectModeToStr")
    static String courtSelectModeToStr(CourtSelectModeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToWaitlistStatus")
    static RegistrationStatusEnum strToWaitlistStatus(String value) {
        return value == null ? null : RegistrationStatusEnum.valueOf(value);
    }

    @Named("waitlistStatusToStr")
    static String waitlistStatusToStr(RegistrationStatusEnum value) {
        return value == null ? null : value.name();
    }

    // ==================== JSON 转换 ====================

    @Named("jsonToCostData")
    static CostData jsonToCostData(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, CostData.class);
    }

    @Named("costDataToJson")
    static String costDataToJson(CostData costData) {
        if (costData == null) {
            return null;
        }
        return JSON.toJSONString(costData);
    }

    // ==================== RegistrationPO ↔ RegistrationData ====================

    @Mapping(target = "status", source = "status", qualifiedByName = "strToWaitlistStatus")
    RegistrationData toRegistrationData(RegistrationPO po);

    List<RegistrationData> toRegistrationDataList(List<RegistrationPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "waitlistStatusToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    RegistrationPO toRegistrationPO(RegistrationData data);
}
