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
import java.util.ArrayList;
import java.util.List;

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
        return new Meetup(data, registrations);
    }

    /**
     * 赛事订场时创建草稿约球（status=DRAFT，全部参与者自动JOINED）。
     * 数据结构与普通发布一致，matchType/maxPlayers/currentPlayers 按参赛人数强制。
     * courtData 非空（TEXT/MAP 模式）时球场信息以球场库数据为准。
     *
     * @param cmd          订场命令（含约球全量字段）
     * @param bookerId     订场人ID（作为草稿创建者）
     * @param courtData    球场库数据，FREE 模式为 null
     * @param participants 比赛参与者
     */
    public static Meetup createTournamentDraft(SubmitBookingCmd cmd, String bookerId, CourtData courtData, List<MatchParticipantData> participants) {
        MeetupData data = MeetupDomainConvertMapper.INSTANCE.toMeetupData(cmd, bookerId, courtData);
        data.setBizId(IdWorker.getIdStr());
        data.setCityName(CityConfig.getCityName(data.getCityCode()));
        applyTournamentParticipants(data, cmd, participants);

        List<RegistrationData> registrations = new ArrayList<>();
        for (MatchParticipantData participant : participants) {
            registrations.add(buildJoinedRegistration(data.getBizId(), participant.getUserId()));
        }
        return new Meetup(data, registrations);
    }

    /**
     * 赛事约球人数/类型按参赛者强制，标题空则给默认值
     */
    private static void applyTournamentParticipants(MeetupData data, SubmitBookingCmd cmd, List<MatchParticipantData> participants) {
        data.setMatchType(participants.size() == 2 ? MatchTypeEnum.SINGLE : MatchTypeEnum.DOUBLE);
        data.setMaxPlayers(participants.size());
        data.setCurrentPlayers(participants.size());
        if (StringUtils.isBlank(data.getTitle())) {
            data.setTitle("赛事约球");
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

