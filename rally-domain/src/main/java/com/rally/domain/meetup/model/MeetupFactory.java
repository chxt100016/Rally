package com.rally.domain.meetup.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.system.CityConfig;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.SubmitBookingCmd;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 约球聚合根工厂
 */
public class MeetupFactory {

    /**
     * 创建新约球（含创建者自动报名）
     *
     * @param cmd       发布命令（含 cityCode）
     * @param userId    创建者 ID
     * @param courtData TEXT/MAP 模式下从球场库查得的球场数据，FREE 模式为 null
     * @return 完整的 Meetup 聚合根（含创建者报名记录）
     */
    public static Meetup create(MeetupPublishCmd cmd, String userId, CourtData courtData) {
        return createInternal(cmd, userId, courtData);
    }

    /** C1：建立普通约球及创建者报名；发布级组合校验仍由 MeetupPolicy 先行完成。 */
    public static Meetup create(MeetupPublishCmd cmd, String userId, CourtData courtData, LocalDateTime now) {
        if (StringUtils.isBlank(userId) || cmd == null || cmd.getStartTime() == null
                || cmd.getStartTime().isBefore(now)) {
            throw new com.rally.domain.auth.exception.BusinessException(
                    com.rally.domain.auth.enums.BizErrorCode.PARAM_ERROR);
        }
        return createInternal(cmd, userId, courtData);
    }

    private static Meetup createInternal(MeetupPublishCmd cmd, String userId, CourtData courtData) {
        if (StringUtils.isBlank(userId) || cmd == null) {
            throw new com.rally.domain.auth.exception.BusinessException(
                    com.rally.domain.auth.enums.BizErrorCode.PARAM_ERROR);
        }
        // 1. 映射 MeetupPublishCmd -> MeetupData（currentPlayers 已在 MapStruct 中设为 1）
        MeetupData data = MeetupDomainConvertMapper.INSTANCE.toMeetupData(cmd, userId, courtData);
        data.setCityName(CityConfig.getCityName(data.getCityCode()));
        data.setBizId(IdWorker.getIdStr());

        // 2. 创建者自动加入报名表，状态为 JOINED
        RegistrationData creatorRegistration = new RegistrationData();
        creatorRegistration.setBizId(IdWorker.getIdStr());
        creatorRegistration.setRallyMeetupId(data.getBizId());
        creatorRegistration.setUserId(userId);
        creatorRegistration.setStatus(RegistrationStatusEnum.JOINED);

        // 3. 如果title为空， 设置title
        if (StringUtils.isBlank(data.getTitle())) {
            data.setTitle(generateTitle(cmd));
        }


        // 4. 组装聚合根
        List<RegistrationData> registrations = new ArrayList<>();
        registrations.add(creatorRegistration);
        Meetup meetup = new Meetup(data, registrations);
        meetup.validateAfterCommand();
        return meetup;
    }

    /**
     * 赛事订场时创建草稿约球（status=DRAFT，全部参与者自动JOINED）。
     * 数据结构与普通发布一致，matchType/maxPlayers/currentPlayers 按参赛人数强制。
     * courtData 非空（TEXT/MAP 模式）时球场信息以球场库数据为准。
     *
     * @param cmd            订场命令（含约球全量字段）
     * @param bookerId       订场人ID（作为草稿创建者）
     * @param courtData      球场库数据，FREE 模式为 null
     * @param participants   比赛参与者
     * @param tournamentName 赛事名称，标题为空时作为默认值
     */
    public static Meetup createTournamentDraft(SubmitBookingCmd cmd, String bookerId, CourtData courtData, List<MatchParticipantData> participants, String tournamentName) {
        return createTournamentDraft(cmd, bookerId, courtData, participants, tournamentName, true);
    }

    /** C2：赛事流程将外部许可显式传入；候选用户去重后建立 JOINED 报名。 */
    public static Meetup createTournamentDraft(SubmitBookingCmd cmd, String bookerId, CourtData courtData,
                                                List<MatchParticipantData> participants, String tournamentName,
                                                boolean tournamentCreationAllowed) {
        if (!tournamentCreationAllowed || StringUtils.isBlank(bookerId) || participants == null
                || participants.stream().filter(Objects::nonNull)
                .map(MatchParticipantData::getUserId).filter(StringUtils::isNotBlank).distinct().findAny().isEmpty()) {
            throw new com.rally.domain.auth.exception.BusinessException(
                    com.rally.domain.auth.enums.BizErrorCode.PARAM_ERROR);
        }
        MeetupData data = MeetupDomainConvertMapper.INSTANCE.toMeetupData(cmd, bookerId, courtData);
        data.setBizId(IdWorker.getIdStr());
        data.setCityName(CityConfig.getCityName(data.getCityCode()));
        List<String> distinctParticipantIds = participants.stream()
                .filter(Objects::nonNull)
                .map(MatchParticipantData::getUserId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        applyTournamentParticipants(data, distinctParticipantIds.size(), tournamentName);

        List<RegistrationData> registrations = new ArrayList<>();
        for (String participantId : distinctParticipantIds) {
            registrations.add(buildJoinedRegistration(data.getBizId(), participantId));
        }
        Meetup meetup = new Meetup(data, registrations);
        meetup.validateAfterCommand();
        return meetup;
    }

    /**
     * 创建赛事线下赛活动：创建人由运营端指定，报名表只写入晋级线下赛的用户。
     * 人数上限按晋级人数强制设置，允许超过普通约球的 16 人限制。
     */
    public static Meetup createTournamentOffline(MeetupPublishCmd cmd, String creatorId, CourtData courtData, List<String> participantUserIds) {
        MeetupData data = MeetupDomainConvertMapper.INSTANCE.toMeetupData(cmd, creatorId, courtData);
        data.setBizId(IdWorker.getIdStr());
        data.setCityName(CityConfig.getCityName(data.getCityCode()));
        data.setMeetupType(MeetupTypeEnum.TOURNAMENT.getCode());
        List<String> distinctParticipantUserIds = participantUserIds.stream()
                .filter(StringUtils::isNotBlank).distinct().toList();
        data.setMaxPlayers(distinctParticipantUserIds.size());
        data.setCurrentPlayers(distinctParticipantUserIds.size());

        List<RegistrationData> registrations = distinctParticipantUserIds.stream()
                .map(userId -> buildJoinedRegistration(data.getBizId(), userId))
                .toList();
        Meetup meetup = new Meetup(data, registrations);
        meetup.validateAfterCommand();
        return meetup;
    }

    /**
     * 赛事约球人数/类型按参赛者强制，标题空则给默认值
     */
    private static void applyTournamentParticipants(MeetupData data, int participantCount, String tournamentName) {
        data.setMatchType(participantCount == 2 ? MatchTypeEnum.SINGLE : MatchTypeEnum.DOUBLE);
        data.setMaxPlayers(participantCount);
        data.setCurrentPlayers(participantCount);
        if (StringUtils.isBlank(data.getTitle())) {
            data.setTitle(tournamentName);
        }
    }

    private static RegistrationData buildJoinedRegistration(String meetupId, String userId) {
        RegistrationData registration = new RegistrationData();
        registration.setBizId(IdWorker.getIdStr());
        registration.setRallyMeetupId(meetupId);
        registration.setUserId(userId);
        registration.setStatus(RegistrationStatusEnum.JOINED);
        return registration;
    }

    private static String generateTitle(MeetupPublishCmd cmd) {
        // 星期几 + 类型，例：星期六约单打 仅女生 需审批
        StringBuilder title = new StringBuilder(weekdayText(cmd.getStartTime().getDayOfWeek())).append("约").append(matchTypeText(cmd.getMatchType()));
        if (cmd.getGenderLimit() == GenderLimitEnum.FEMALE) {
            title.append(" 仅女生");
        } else if (cmd.getGenderLimit() == GenderLimitEnum.MALE) {
            title.append(" 仅男生");
        }
        if (cmd.getJoinMode() == JoinModeEnum.APPROVAL) {
            title.append(" 需审批");
        }
        return title.toString();
    }

    private static String weekdayText(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private static String matchTypeText(MatchTypeEnum matchType) {
        return switch (matchType) {
            case SINGLE -> "单打";
            case DOUBLE -> "双打";
            case RALLY -> "拉球";
        };
    }
}
