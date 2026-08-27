package com.rally.tournament.bookingsubmit.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupFactory;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.SubmitBookingCmd;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.utils.Assert;
import com.rally.notify.NotificationEventId;
import com.rally.notify.TournamentNotifyAssembler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务活动 save-booking：新建或更新赛事赛约，并在首次提交时推进比赛确认。
 */
@Component
@RequiredArgsConstructor
public class SaveBookingActivity {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final MeetupRepository meetupRepository;
    private final CourtRepository courtRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 沿用现有提交赛约主链，保留其校验、版本条件、事务与持久化顺序。
     */
    @Transactional(rollbackFor = Exception.class)
    public String execute(SubmitBookingCmd command, String operatorId) {
        // A1-A4：既有领域流程保留比赛/赛事/操作人/赛约校验，场地解析和跨聚合事务写入语义。
        String meetupId = saveBooking(command, operatorId);

        /*
         * A5：与主线保持一致，用比赛及聚合产生的提交时间构造稳定事件。
         * SCHEDULED 内修改会得到相同事件 ID，由 delivery 唯一日志阻止重复发送；
         * 调度发生在事务提交后，且任一通知异常均不向赛约提交传播。
         */
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(command.getMatchId());
        TournamentData tournament = tournamentRepository.findByBizId(
                match.getData().getTournamentId());
        MeetupData booking = meetupRepository.findByBizId(meetupId);
        Object submissionRef = match.getData().getScheduleSubmittedTime() == null
                ? meetupId
                : match.getData().getScheduleSubmittedTime().withNano(0);
        notificationDeliveryService.notify(
                NotificationEventId.of(
                        NoticeScene.TOURNAMENT_BOOKING_SUBMITTED,
                        match.getMatchId(),
                        submissionRef),
                NotifyBizType.TOURNAMENT,
                tournament.getBizId(),
                NoticeScene.TOURNAMENT_BOOKING_SUBMITTED,
                otherParticipantIds(match, operatorId),
                TournamentNotifyAssembler.bookingSubmittedData(
                        tournament.getTournamentName(),
                        booking.getStartTime(),
                        booking.getCourtName()));
        return meetupId;
    }

    private String saveBooking(SubmitBookingCmd command, String operatorId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(
                command.getMatchId());
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        CourtData court = resolveCourt(command.getCourtSelectMode(), command.getCourtId());
        TournamentData tournament = tournamentRepository.findByBizId(
                match.getData().getTournamentId());
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        if (StringUtils.isNotBlank(command.getMeetupId())) {
            return updateBooking(command, operatorId, match, court, tournament);
        }

        match.submitBooking(operatorId);
        Meetup draft = MeetupFactory.createTournamentDraft(
                command,
                operatorId,
                court,
                match.getParticipants(),
                tournament.getTournamentName());
        meetupRepository.save(draft);
        match.getData().setMeetupId(draft.getMeetupId());
        persistMatch(match);
        return draft.getMeetupId();
    }

    private String updateBooking(
            SubmitBookingCmd command,
            String operatorId,
            TournamentMatch match,
            CourtData court,
            TournamentData tournament) {
        MeetupData meetup = meetupRepository.findByBizId(command.getMeetupId());
        Assert.notNull(meetup, BizErrorCode.MEETUP_NOT_FOUND);
        Assert.eq(match.getData().getMeetupId(), command.getMeetupId(),
                BizErrorCode.TOURNAMENT_BOOKING_MEETUP_MISMATCH);
        Assert.eq(meetup.getCreatorId(), operatorId, BizErrorCode.NOT_CREATOR);

        TournamentMatchStatusEnum status = match.getData().getStatus();
        Assert.isTrue(
                status == TournamentMatchStatusEnum.BOOKING
                        || status == TournamentMatchStatusEnum.SCHEDULED,
                BizErrorCode.MEETUP_TOURNAMENT_EDIT_FORBIDDEN);
        MeetupDomainConvertMapper.INSTANCE.updateTournamentMeetupData(
                meetup, command, court);
        if (StringUtils.isBlank(meetup.getTitle())) {
            meetup.setTitle(tournament.getTournamentName());
        }
        meetupRepository.save(meetup);
        if (status == TournamentMatchStatusEnum.BOOKING) {
            match.submitBooking(operatorId);
            persistMatch(match);
        }
        return meetup.getBizId();
    }

    private CourtData resolveCourt(CourtSelectModeEnum mode, String courtId) {
        if ((mode == CourtSelectModeEnum.TEXT || mode == CourtSelectModeEnum.MAP)
                && StringUtils.isNotBlank(courtId)) {
            return courtRepository.findByBizId(courtId);
        }
        return null;
    }

    private void persistMatch(TournamentMatch match) {
        if (!matchRepository.updateWithVersion(match.getData())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
    }

    private List<String> otherParticipantIds(
            TournamentMatch match,
            String excludedUserId) {
        return match.getParticipants().stream()
                .map(MatchParticipantData::getUserId)
                .filter(userId -> !userId.equals(excludedUserId))
                .distinct()
                .toList();
    }
}
