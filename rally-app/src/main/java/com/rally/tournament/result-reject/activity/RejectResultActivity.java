package com.rally.tournament.resultreject.activity;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.tournament.enums.ResultRejectReasonEnum;
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
 * 业务活动 reject-result：拒绝待确认赛果，累计赛段次数并让有效报名回池。
 */
@Component
@RequiredArgsConstructor
public class RejectResultActivity {

    private final TournamentMatchRejectionSupport rejectionSupport;
    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 保持 confirm=false 链路的参与资格、限额、版本化保存和跨聚合事务语义。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            ResultRejectReasonEnum rejectReason) {
        // A1-A4：固定走赛果拒绝分支；任一比赛、参与者、计数、报名或赛约写入失败均回滚。
        TournamentMatch rejectedMatch = rejectionSupport.requireMatch(matchId);
        TournamentData tournamentData = rejectionSupport.requireTournament(
                rejectedMatch.getData().getTournamentId());
        TournamentEntry entry = rejectionSupport.requireEntry(
                rejectedMatch.getData().getTournamentId(), participantUserId);
        rejectedMatch.confirmResult(
                participantUserId,
                false,
                rejectReason,
                tournamentData.getQualifierRejectLimit(),
                tournamentData.getMainDrawRejectLimit(),
                entry.getData().getStage(),
                rejectionSupport.rejectCount(entry));
        rejectionSupport.persistMatch(rejectedMatch, true);
        if (rejectReason != null) {
            rejectionSupport.incrementRejectCount(entry);
            rejectionSupport.settleRejectedMatch(rejectedMatch);
        }

        // A5：事务提交后直接尝试通知其他参与者，不申领或变更订阅配额。
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
