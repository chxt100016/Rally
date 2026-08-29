package com.rally.tournament.singlematchcancel.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.entry.TournamentEntry;
import com.rally.domain.tournament.entry.TournamentEntryPersistence;
import com.rally.domain.tournament.entry.TournamentEntryReleaseReason;
import com.rally.domain.tournament.entry.TournamentEntryState;
import com.rally.domain.tournament.match.TournamentMatchCancellationParticipant;
import com.rally.domain.tournament.match.TournamentMatchCancellationSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 release-cancelled-match-entries：将被运营取消比赛中的报名释放回匹配池。
 */
@Component
@RequiredArgsConstructor
public class ReleaseCancelledMatchEntriesActivity {

    private final TournamentEntryPersistence entryPersistence;

    /**
     * 只处理取消快照明确包含的参与者；缺失报名和非 IN_MATCH 状态均幂等跳过。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(TournamentMatchCancellationSnapshot cancellationSnapshot) {
        try {
            // A1-A3：逐个按赛事和用户自然键恢复聚合，不按 entryNo 扩展其他报名。
            for (TournamentMatchCancellationParticipant participant
                    : cancellationSnapshot.participants()) {
                TournamentEntryState stored = entryPersistence.findByTournamentAndUser(
                        cancellationSnapshot.tournamentId(), participant.userId());
                if (stored == null) {
                    continue;
                }
                TournamentEntry.restore(stored).releaseToWaiting(
                        TournamentEntryReleaseReason.ADMIN_CANCELLED,
                        false,
                        0,
                        entryPersistence);
            }
            // A4：遍历完成即返回；实际保存失败由异常触发外层事务整体回滚。
        } catch (RuntimeException exception) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "释放取消比赛报名失败");
        }
    }
}
