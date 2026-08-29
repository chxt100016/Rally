package com.rally.tournament.singlematchcancel.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.match.CancelTournamentMatchCommand;
import com.rally.domain.tournament.match.TournamentMatch;
import com.rally.domain.tournament.match.TournamentMatchCancellationSnapshot;
import com.rally.domain.tournament.match.TournamentMatchDomainException;
import com.rally.domain.tournament.match.TournamentMatchPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 delete-cancellable-match：删除运营指定的未完成比赛并返回联动快照。
 */
@Component
@RequiredArgsConstructor
public class DeleteCancellableMatchActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchPersistence matchPersistence;

    /**
     * 锁定、快照和条件物理删除共用外层事务，供后续活动继续联动。
     */
    @Transactional(rollbackFor = Exception.class)
    public TournamentMatchCancellationSnapshot execute(String tournamentId, int matchNo) {
        // A1：只确认赛事存在，不限制赛事状态、当前轮次或比赛所属轮次。
        if (tournamentRepository.findByBizId(tournamentId) == null) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_NOT_FOUND);
        }

        try {
            // A2-A5：聚合负责自然键锁定、最新状态判定、快照生成与条件物理删除。
            return TournamentMatch.cancel(
                    new CancelTournamentMatchCommand(tournamentId, matchNo),
                    matchPersistence);
        } catch (TournamentMatchDomainException exception) {
            throw toBusinessException(exception);
        } catch (RuntimeException exception) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "比赛取消失败");
        }
    }

    private BusinessException toBusinessException(TournamentMatchDomainException exception) {
        return switch (exception.getErrorIdentifier()) {
            case TournamentMatch.TOURNAMENT_MATCH_NOT_FOUND ->
                    new BusinessException(BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
            case TournamentMatch.TOURNAMENT_MATCH_CANCEL_FORBIDDEN ->
                    new BusinessException(
                            BizErrorCode.TOURNAMENT_MATCH_CANCEL_FORBIDDEN,
                            "已完成比赛不能取消");
            case TournamentMatch.TOURNAMENT_MATCH_VERSION_CONFLICT ->
                    new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
            default -> new BusinessException(BizErrorCode.OPERATION_FAILED, exception.getMessage());
        };
    }
}
