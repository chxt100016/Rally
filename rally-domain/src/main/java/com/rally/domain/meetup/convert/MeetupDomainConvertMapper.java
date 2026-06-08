package com.rally.domain.meetup.convert;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupPublishCmd;
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
     */

    @Mapping(target = "creatorId", source = "userId")
    @Mapping(target = "endTime", expression = "java(calculateEndTime(cmd.getStartTime(), cmd.getDuration()))")
    @Mapping(target = "status", expression = "java(com.rally.domain.meetup.enums.MeetupStatusEnum.OPEN)")
    MeetupData toMeetupData(MeetupPublishCmd cmd, String userId);

    /**
     * 更新 MeetupData（忽略 null 值）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "endTime", expression = "java(cmd.getStartTime() != null ? calculateEndTime(cmd.getStartTime(), cmd.getDuration()) : data.getEndTime())")
    void updateMeetupData(@MappingTarget MeetupData data, MeetupPublishCmd cmd);


    /**
     * MeetupData -> MeetupCardDTO
     */
    @Mapping(target = "meetupId", source = "bizId")
    MeetupCardDTO toMeetupCardDTO(MeetupData data);

    /**
     * 计算结束时间
     */
    default LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal duration) {
        return startTime.plusHours(duration.longValue())
                .plusMinutes((duration.remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue());
    }

}
