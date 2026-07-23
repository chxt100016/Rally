package com.rally.domain.meetup.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.system.CityConfig;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentMatchData;
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
     * 从赛事比赛创建约球（全部参与者自动JOINED）
     * 场地信息（courtName/courtAddress/courtLng/courtLat/城市）已在比赛 submitBooking 时按球场库数据校正并落库，直接取用，不再反查
     */
    public static Meetup createFromTournamentMatch(TournamentMatchData matchData, List<MatchParticipantData> participants) {
        MeetupData data = new MeetupData();
        data.setBizId(IdWorker.getIdStr());
        data.setMeetupType(MeetupTypeEnum.TOURNAMENT.getCode());
        data.setCreatorId(participants.isEmpty() ? null : participants.get(0).getUserId());
        data.setTitle("赛事约球");
        data.setMatchType(participants.size() == 2 ? MatchTypeEnum.SINGLE : MatchTypeEnum.DOUBLE);
        data.setMaxPlayers(participants.size());
        data.setCurrentPlayers(participants.size());
        data.setStartTime(matchData.getScheduledStartTime());
        data.setEndTime(matchData.getScheduledStartTime().plusHours(matchData.getScheduledDuration().longValue()));
        data.setDuration(new java.math.BigDecimal(matchData.getScheduledDuration()));
        data.setCourtName(matchData.getCourtName());
        data.setCourtAddress(matchData.getCourtAddress());
        data.setCourtLng(matchData.getCourtLng());
        data.setCourtLat(matchData.getCourtLat());
        data.setCourtSelectMode(matchData.getCourtSelectMode());
        data.setCourtId(matchData.getCourtId());
        data.setCityCode(matchData.getCourtCityCode());
        data.setCityName(matchData.getCourtCityName());
        data.setStatus(MeetupStatusEnum.OPEN);
        data.setJoinMode(JoinModeEnum.DIRECT);
        data.setGenderLimit(GenderLimitEnum.ANY);

        List<RegistrationData> registrations = new ArrayList<>();
        for (MatchParticipantData participant : participants) {
            RegistrationData registration = new RegistrationData();
            registration.setBizId(IdWorker.getIdStr());
            registration.setRallyMeetupId(data.getBizId());
            registration.setUserId(participant.getUserId());
            registration.setStatus(RegistrationStatusEnum.JOINED);
            registrations.add(registration);
        }

        return new Meetup(data, registrations);
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

