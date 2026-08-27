package com.rally.tournament.bookingreject.activity;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.tournament.enums.ScheduleRejectReasonEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.tournament.shared.TournamentMatchRejectionSupport;
import com.rally.notify.NotificationEventId;
import com.rally.notify.TournamentNotifyAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务活动 reject-await-booking-by-booker：订场人超时后终止订场并释放报名。
 */
@Component
@RequiredArgsConstructor
public class RejectAwaitBookingByBookerActivity {

    private final TournamentMatchRejectionSupport rejectionSupport;
    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 保持既有拒赛链路的事务与写入顺序：比赛及参与者、草稿赛约、在赛报名。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String bookerUserId,
            ScheduleRejectReasonEnum rejectReason) {
        // A1-A4：由既有流程保留 BOOKING/订场人/超时校验、版本保存及跨聚合结算语义。
        TournamentMatch rejectedMatch = rejectionSupport.requireMatch(matchId);
        rejectedMatch.rejectOnAwaitBooking(bookerUserId, rejectReason);
        rejectionSupport.persistRejectedMatch(rejectedMatch);

        // A5：NotificationDeliveryService 在事务提交后异步触达，且通知失败在其内部容错。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        TournamentData tournament = tournamentRepository.findByBizId(
                match.getData().getTournamentId());
        notificationDeliveryService.notify(
                NotificationEventId.of(NoticeScene.TOURNAMENT_REJECTED, matchId),
                NotifyBizType.TOURNAMENT,
                tournament.getBizId(),
                NoticeScene.TOURNAMENT_REJECTED,
                otherParticipantIds(match, bookerUserId),
                TournamentNotifyAssembler.rejectedData(
                        tournament.getTournamentName()));
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
