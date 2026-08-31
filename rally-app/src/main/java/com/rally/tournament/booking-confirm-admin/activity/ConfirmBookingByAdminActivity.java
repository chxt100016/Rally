package com.rally.tournament.bookingconfirmadmin.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务活动 confirm-booking-by-admin：运营按赛事编号和比赛序号一次性代确认全部未确认参与者赛约，
 * 推进比赛并开放草稿赛约。
 */
@Component
@RequiredArgsConstructor
public class ConfirmBookingByAdminActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchRepository matchRepository;
    private final MeetupRepository meetupRepository;
    private final RegistrationRepository registrationRepository;
    private final MeetupDomainService meetupDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId, Integer matchNo) {
        // A1：只确认赛事身份存在，不要求赛事状态。
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        // A2：按自然键定位比赛后锁定读取最新根及全部参与者；比赛不是 SCHEDULED 时拒绝代确认。
        String matchId = locateMatchId(tournamentId, matchNo);
        Assert.notNull(matchId, BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
        TournamentMatch match = matchRepository.findByBizIdWithParticipantsForUpdate(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
        Assert.eq(match.getData().getStatus(), TournamentMatchStatusEnum.SCHEDULED, BizErrorCode.TOURNAMENT_INVALID_SCHEDULE_CONFIRM);

        // A3：逐个覆盖仍非 CONFIRMED 的参与者为 CONFIRMED 并刷新确认时间；已 CONFIRMED 的保持原状。
        LocalDateTime now = LocalDateTime.now();
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participant.getConfirmStatus() != ConfirmStatusEnum.CONFIRMED) {
                match.confirmSchedule(participant.getUserId(), true, null, null, 0, 0, null, 0);
                participant.setConfirmTime(now);
            }
        }
        // 兜底：参与者为空或已全部 CONFIRMED 的异常存量数据，直接推进比赛。
        match.advanceIfAllConfirmed();

        // A4：以当前版本统一保存比赛根与全部参与关系；全员确认时比赛已进入 PENDING_PLAY。
        boolean updated = matchRepository.updateWithVersion(match.getData());
        Assert.isTrue(updated, BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        matchRepository.saveParticipants(match.getParticipants());

        // A5：仅在比赛进入 PENDING_PLAY 且关联赛约仍为 DRAFT 且未过期时开放为 OPEN。
        if (match.getData().getStatus() == TournamentMatchStatusEnum.PENDING_PLAY) {
            openDraftMeetupIfNeeded(match.getData().getMeetupId(), now);
        }
    }

    private String locateMatchId(String tournamentId, Integer matchNo) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .filter(data -> matchNo.equals(data.getMatchNo()))
                .map(TournamentMatchData::getBizId)
                .findFirst()
                .orElse(null);
    }

    private void openDraftMeetupIfNeeded(String meetupId, LocalDateTime now) {
        if (meetupId == null) {
            return;
        }
        MeetupData meetupData = meetupRepository.findByBizId(meetupId);
        if (meetupData == null || meetupData.getStatus() != MeetupStatusEnum.DRAFT) {
            return;
        }
        if (!now.isBefore(meetupData.getStartTime())) {
            return;
        }
        Meetup meetup = new Meetup(meetupData, registrationRepository.findByMeetupId(meetupId));
        meetupDomainService.openTournamentDraft(meetup, true, now);
    }
}
