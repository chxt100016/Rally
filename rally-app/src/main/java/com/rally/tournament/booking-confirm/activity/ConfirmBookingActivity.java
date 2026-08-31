package com.rally.tournament.bookingconfirm.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务活动 confirm-booking：记录本人接受赛约，并在全员确认后开放比赛与草稿赛约。
 */
@Component
@RequiredArgsConstructor
public class ConfirmBookingActivity {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final MeetupRepository meetupRepository;

    /**
     * 沿用 schedule-confirm 确认分支的事务和持久化顺序：比赛根、参与者、可选草稿赛约。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            LocalDateTime confirmTime) {
        Assert.notNull(confirmTime, BizErrorCode.PARAM_ERROR);

        // A1：先完整加载比赛；不存在与非参与者均维持报名不存在的对外错误。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        /*
         * A2：保持 main 的确认分支读取顺序和历史兼容。只有已处于 SCHEDULED
         * 且关联记录可读时才检查；严格早于本次冻结时间才过期，恰等继续。
         * 是否执行该过期校验由系统配置开关控制，默认不校验。
         */
        if (match.getData().getStatus() == TournamentMatchStatusEnum.SCHEDULED) {
            assertLinkedMeetupNotExpired(match.getData().getMeetupId(), confirmTime);
        }

        TournamentData tournament = tournamentRepository.findByBizId(
                match.getData().getTournamentId());
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        TournamentEntryData entry = entryRepository.findByTournamentAndUser(
                match.getData().getTournamentId(), participantUserId);
        Assert.notNull(entry, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        int rejectCount = entry.getStage() == TournamentEntryStageEnum.QUALIFY
                ? entry.getQualifierRejectCount()
                : entry.getMainDrawRejectCount();

        // A3-A4：聚合覆盖本人确认并按全体确认结果保持 SCHEDULED 或推进 PENDING_PLAY。
        match.confirmSchedule(
                participantUserId,
                true,
                null,
                null,
                tournament.getQualifierRejectLimit(),
                tournament.getMainDrawRejectLimit(),
                entry.getStage(),
                rejectCount);
        replaceParticipantConfirmTime(match, participantUserId, confirmTime);

        boolean updated = matchRepository.updateWithVersion(match.getData());
        if (!updated) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        // A5：全员确认后仅把仍存在的 DRAFT 赛约改为 OPEN；缺失或其他状态均跳过。
        if (match.getData().getStatus() == TournamentMatchStatusEnum.PENDING_PLAY) {
            activateDraftMeetup(match.getData().getMeetupId());
        }
    }

    private void assertLinkedMeetupNotExpired(
            String meetupId,
            LocalDateTime confirmTime) {
        // 开关默认关闭，只有显式开启才校验赛约开始时间不能已过去。
        if (!SystemConfig.getBoolean(SystemConfigKey.TOURNAMENT_BOOKING_START_TIME_EXPIRE_CHECK.getKey())) {
            return;
        }
        if (meetupId == null) {
            return;
        }
        MeetupData meetup = meetupRepository.findByBizId(meetupId);
        if (meetup != null && meetup.getStartTime().isBefore(confirmTime)) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }
    }

    private void replaceParticipantConfirmTime(
            TournamentMatch match,
            String participantUserId,
            LocalDateTime confirmTime) {
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participantUserId.equals(participant.getUserId())) {
                participant.setConfirmTime(confirmTime);
                return;
            }
        }
    }

    private void activateDraftMeetup(String meetupId) {
        if (meetupId == null) {
            return;
        }
        MeetupData meetup = meetupRepository.findByBizId(meetupId);
        if (meetup != null && meetup.getStatus() == MeetupStatusEnum.DRAFT) {
            meetup.setStatus(MeetupStatusEnum.OPEN);
            meetupRepository.save(meetup);
        }
    }
}
