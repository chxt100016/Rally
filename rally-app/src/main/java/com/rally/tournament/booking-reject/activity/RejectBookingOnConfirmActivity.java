package com.rally.tournament.bookingreject.activity;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.tournament.enums.ScheduleRejectReasonEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.notify.NotificationEventId;
import com.rally.notify.TournamentNotifyAssembler;
import com.rally.tournament.shared.TournamentMatchRejectionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务活动 reject-booking-on-confirm：参与者确认赛约时直接拒赛并累计当前赛段次数。
 */
@Component
@RequiredArgsConstructor
public class RejectBookingOnConfirmActivity {

    private final TournamentMatchRejectionSupport rejectionSupport;
    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 保持 schedule-confirm 拒赛分支的事务与写入顺序：比赛及参与者、本人计数、草稿赛约和其他在赛报名。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            ScheduleRejectReasonEnum rejectReason) {
        // A1-A4：固定走 confirm=false/reject 分支，由既有流程保留身份、状态、限额和跨聚合结算语义。
        TournamentMatch rejectedMatch = rejectionSupport.requireMatch(matchId);
        TournamentData tournamentData = rejectionSupport.requireTournament(
                rejectedMatch.getData().getTournamentId());
        TournamentEntry entry = rejectionSupport.requireEntry(
                rejectedMatch.getData().getTournamentId(), participantUserId);
        rejectedMatch.confirmSchedule(
                participantUserId,
                false,
                rejectReason,
                null,
                tournamentData.getQualifierRejectLimit(),
                tournamentData.getMainDrawRejectLimit(),
                entry.getData().getStage(),
                rejectionSupport.rejectCount(entry));
        rejectionSupport.persistMatch(rejectedMatch, true);
        if (rejectedMatch.getData().getStatus() == TournamentMatchStatusEnum.REJECTED) {
            if (rejectReason != null) {
                rejectionSupport.incrementRejectCount(entry);
            }
            rejectionSupport.settleRejectedMatch(rejectedMatch);
        }

        // A5：NotificationDeliveryService 在事务提交后异步触达，且通知失败在其内部容错。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        TournamentData tournament = tournamentRepository.findByBizId(
                match.getData().getTournamentId());
        notificationDeliveryService.notify(
                NotificationEventId.of(NoticeScene.TOURNAMENT_REJECTED, matchId),
                NotifyBizType.TOURNAMENT,
                tournament.getBizId(),
                NoticeScene.TOURNAMENT_REJECTED,
                otherParticipantIds(match, participantUserId),
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
