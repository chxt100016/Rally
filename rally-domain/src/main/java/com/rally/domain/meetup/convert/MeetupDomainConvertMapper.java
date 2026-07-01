package com.rally.domain.meetup.convert;

import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupPublishCmd;
import com.rally.domain.utils.AddressUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 约球域 MapStruct 转换器
 */
@Mapper
public interface MeetupDomainConvertMapper {

    MeetupDomainConvertMapper INSTANCE = Mappers.getMapper(MeetupDomainConvertMapper.class);

    /**
     * MeetupPublishCmd -> MeetupData
     * courtData 非空时（TEXT/MAP 模式），球场信息以球场库数据为准
     */
    @Mapping(target = "creatorId", source = "userId")
    @Mapping(target = "cityCode", source = "cmd.cityCode")
    @Mapping(target = "endTime", expression = "java(calculateEndTime(cmd.getStartTime(), cmd.getDuration()))")
    @Mapping(target = "status", expression = "java(com.rally.domain.meetup.enums.MeetupStatusEnum.OPEN)")
    @Mapping(target = "courtName", expression = "java(courtData != null ? courtData.getName() : cmd.getCourtName())")
    @Mapping(target = "courtAddress", expression = "java(resolveCourtAddress(cmd, courtData))")
    @Mapping(target = "courtLng", expression = "java(courtData != null ? courtData.getLng() : cmd.getCourtLng())")
    @Mapping(target = "courtLat", expression = "java(courtData != null ? courtData.getLat() : cmd.getCourtLat())")
    @Mapping(target = "districtName", expression = "java(resolveDistrictName(cmd, courtData))")
    MeetupData toMeetupData(MeetupPublishCmd cmd, String userId, CourtData courtData);

    /**
     * 更新 MeetupData（忽略 null 值）
     * courtData 非空时（TEXT/MAP 模式），球场信息以球场库数据为准
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "cityCode", source = "cmd.cityCode")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "endTime", expression = "java(cmd.getStartTime() != null ? calculateEndTime(cmd.getStartTime(), cmd.getDuration()) : data.getEndTime())")
    @Mapping(target = "courtName", expression = "java(courtData != null ? courtData.getName() : cmd.getCourtName())")
    @Mapping(target = "courtAddress", expression = "java(resolveCourtAddress(cmd, courtData))")
    @Mapping(target = "courtLng", expression = "java(courtData != null ? courtData.getLng() : cmd.getCourtLng())")
    @Mapping(target = "courtLat", expression = "java(courtData != null ? courtData.getLat() : cmd.getCourtLat())")
    @Mapping(target = "districtName", expression = "java(resolveDistrictName(cmd, courtData))")
    void updateMeetupData(@MappingTarget MeetupData data, MeetupPublishCmd cmd, CourtData courtData);

    /**
     * 计算结束时间
     */
    default LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal duration) {
        return startTime.plusHours(duration.longValue())
                .plusMinutes((duration.remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue());
    }

    /**
     * 球场地址：courtData 非空取球场库地址，否则取前端传入地址
     */
    default String resolveCourtAddress(MeetupPublishCmd cmd, CourtData courtData) {
        return courtData != null ? courtData.getAddress() : cmd.getCourtAddress();
    }

    /**
     * 区县名：courtData 有区县名时取球场库数据，否则从最终地址中解析
     */
    default String resolveDistrictName(MeetupPublishCmd cmd, CourtData courtData) {
        if (courtData != null && StringUtils.isNotBlank(courtData.getDistrictName())) {
            return courtData.getDistrictName();
        }
        return AddressUtils.getDistrict(resolveCourtAddress(cmd, courtData));
    }

}
