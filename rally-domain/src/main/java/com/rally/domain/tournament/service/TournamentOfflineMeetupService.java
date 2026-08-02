package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.GenderLimitEnum;
import com.rally.domain.meetup.enums.JoinModeEnum;
import com.rally.domain.meetup.enums.LevelModeEnum;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupPublishCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupPolicy;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentOfflineMeetupCmd;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 赛事线下赛活动创建流程。 */
@Service
@RequiredArgsConstructor
public class TournamentOfflineMeetupService {

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final MeetupPolicy meetupPolicy;
    private final MeetupDomainService meetupDomainService;

    public String create(TournamentOfflineMeetupCmd cmd) {
        TournamentData tournament = tournamentRepository.findByBizId(cmd.getTournamentId());
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);
        if (tournament.getCurrentRound() != tournament.getOfflineFromRound()) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_STATUS_ILLEGAL, "赛事尚未进入线下赛阶段");
        }
        if (tournament.getOfflineMeetupId() != null) {
            throw new BusinessException(BizErrorCode.DATA_DUPLICATE, "线下赛活动已创建");
        }

        List<String> participantUserIds = tournamentEntryRepository.findByTournamentId(cmd.getTournamentId()).stream()
                .filter(entry -> entry.getCurrentRound() == tournament.getOfflineFromRound())
                .map(entry -> entry.getUserId())
                .distinct()
                .toList();
        if (participantUserIds.isEmpty()) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "没有达到线下赛阶段的参赛者");
        }

        MeetupPublishCmd publishCmd = toMeetupPublishCmd(cmd, tournament, participantUserIds.size());
        meetupPolicy.assertTournamentOfflinePublish(publishCmd);
        Meetup meetup = meetupDomainService.saveTournamentOffline(cmd.getCreatorId(), publishCmd, participantUserIds);
        if (!tournamentRepository.bindOfflineMeetupIfAbsent(cmd.getTournamentId(), meetup.getMeetupId())) {
            throw new BusinessException(BizErrorCode.DATA_DUPLICATE, "线下赛活动已创建");
        }
        return meetup.getMeetupId();
    }

    private MeetupPublishCmd toMeetupPublishCmd(TournamentOfflineMeetupCmd cmd, TournamentData tournament, int participantCount) {
        if (tournament.getNtrpLevel() == null || tournament.getNtrpLevel().isBlank()) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_CONFIG_INCOMPLETE, "赛事 NTRP 等级不能为空");
        }
        BigDecimal ntrpLevel;
        try {
            ntrpLevel = new BigDecimal(tournament.getNtrpLevel());
        } catch (NumberFormatException exception) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_CONFIG_INCOMPLETE, "赛事 NTRP 等级不是有效数值");
        }

        MeetupPublishCmd publishCmd = new MeetupPublishCmd();
        publishCmd.setTitle(tournament.getTournamentName());
        publishCmd.setMatchType(tournament.getMatchType());
        publishCmd.setMaxPlayers(participantCount);
        publishCmd.setStartTime(cmd.getStartTime());
        publishCmd.setDuration(cmd.getDuration());
        publishCmd.setCourtName(cmd.getCourtName());
        publishCmd.setCourtAddress(cmd.getCourtAddress());
        publishCmd.setCourtSelectMode(cmd.getCourtSelectMode());
        publishCmd.setCourtId(cmd.getCourtId());
        publishCmd.setCityCode(cmd.getCityCode());
        publishCmd.setDistrictCode(cmd.getDistrictCode());
        publishCmd.setCourtLng(cmd.getCourtLng());
        publishCmd.setCourtLat(cmd.getCourtLat());
        publishCmd.setCourtIndex(cmd.getCourtIndex());
        publishCmd.setLevelMode(LevelModeEnum.EXACT);
        publishCmd.setLevelMin(ntrpLevel);
        publishCmd.setLevelMax(ntrpLevel);
        publishCmd.setGenderLimit(GenderLimitEnum.ANY);
        publishCmd.setJoinMode(JoinModeEnum.APPROVAL);
        publishCmd.setCostItems(List.of());
        return publishCmd;
    }
}
