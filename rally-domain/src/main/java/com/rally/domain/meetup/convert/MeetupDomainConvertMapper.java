package com.rally.domain.meetup.convert;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupVO;
import com.rally.domain.meetup.model.PublishCmd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
     * PublishCmd -> MeetupData
     */

    @Mapping(target = "creatorId", source = "userId")
    @Mapping(target = "title", expression = "java(cmd.getTitle() != null ? cmd.getTitle() : generateTitle(cmd))")
    @Mapping(target = "endTime", expression = "java(calculateEndTime(cmd.getStartTime(), cmd.getDuration()))")
    @Mapping(target = "levelValue", expression = "java(buildLevelValue(cmd))")
    @Mapping(target = "status", expression = "java(com.rally.domain.meetup.enums.MeetupStatusEnum.OPEN)")
    MeetupData toMeetupData(PublishCmd cmd, String userId, String cityCode);

    /**
     * MeetupData -> MeetupVO
     */
    @Mapping(target = "meetupId", expression = "java(data.getBizId())")
    @Mapping(target = "perPersonCost", expression = "java(calculatePerPersonCost(data))")
    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "actionState", ignore = true)
    @Mapping(target = "quitWillPenalize", expression = "java(calculateQuitWillPenalize(data, currentUserId))")
    @Mapping(target = "creatorNickname", ignore = true)
    @Mapping(target = "creatorAvatarUrl", ignore = true)
    @Mapping(target = "creatorNtrp", ignore = true)
    @Mapping(target = "participants", ignore = true)
    MeetupVO toMeetupVO(MeetupData data, String currentUserId);

    /**
     * MeetupData -> MeetupVO（无用户ID版本）
     */
    @Mapping(target = "meetupId", expression = "java(data.getBizId())")
    @Mapping(target = "perPersonCost", expression = "java(calculatePerPersonCost(data))")
    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "actionState", ignore = true)
    @Mapping(target = "quitWillPenalize", ignore = true)
    @Mapping(target = "creatorNickname", ignore = true)
    @Mapping(target = "creatorAvatarUrl", ignore = true)
    @Mapping(target = "creatorNtrp", ignore = true)
    @Mapping(target = "participants", ignore = true)
    MeetupVO toMeetupVO(MeetupData data);

    /**
     * 生成标题
     */
    default String generateTitle(PublishCmd cmd) {
        String type = cmd.getMatchType().name();
        String dayName = cmd.getStartTime().getDayOfWeek().getValue() + "";
        String time = cmd.getStartTime().toLocalTime().toString().substring(0, 5);
        String court = cmd.getCourtName() != null ? cmd.getCourtName() : "待定";
        return type + "·" + dayName + " " + time + "·" + court;
    }

    /**
     * 计算结束时间
     */
    default LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal duration) {
        return startTime.plusHours(duration.longValue())
                .plusMinutes((duration.remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue());
    }

    /**
     * 构建水平值
     */
    default String buildLevelValue(PublishCmd cmd) {
        if (cmd.getLevelMode() == null) {
            return null;
        }
        return switch (cmd.getLevelMode()) {
            case RANGE -> cmd.getLevelMin() + ":" + cmd.getLevelMax();
            case EXACT -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case ABOVE -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case BELOW -> cmd.getLevelMax() != null ? cmd.getLevelMax().toString() : null;
        };
    }

    /**
     * 计算每人费用
     */
    default Integer calculatePerPersonCost(MeetupData data) {
        if (data.getCostItems() == null || data.getCostItems().isEmpty() || data.getMaxPlayers() == null) {
            return null;
        }
        int totalAmount = data.getCostItems().stream()
                .mapToInt(item -> item.getTotalAmount() != null ? item.getTotalAmount() : 0)
                .sum();
        return (int) Math.ceil((double) totalAmount / data.getMaxPlayers());
    }

    /**
     * 计算退出是否扣分
     */
    default boolean calculateQuitWillPenalize(MeetupData data, String currentUserId) {
        if (currentUserId == null || currentUserId.equals(data.getCreatorId())) {
            return false;
        }
        long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
        return hoursUntilStart < 6;
    }


}
