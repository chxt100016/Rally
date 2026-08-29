package com.rally.tournament.unmatchedentryelimination.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.entry.TournamentEntry;
import com.rally.domain.tournament.entry.TournamentEntryDomainException;
import com.rally.domain.tournament.entry.TournamentEntryPersistence;
import com.rally.domain.tournament.entry.TournamentEntryRound;
import com.rally.domain.tournament.entry.TournamentEntryState;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.unmatchedentryelimination.SingleEntryEliminationDecision;
import com.rally.domain.tournament.unmatchedentryelimination.SingleEntrySnapshot;
import com.rally.domain.tournament.unmatchedentryelimination.UnmatchedEntryEliminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 eliminate-unmatched-entry-units：淘汰指定用户的当前轮次未入赛报名。
 */
@Component
@RequiredArgsConstructor
public class EliminateUnmatchedEntryUnitsActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchRepository matchRepository;
    private final TournamentEntryPersistence entryPersistence;
    private final UnmatchedEntryEliminationService eliminationService;

    /** A1-A6 在同一事务中锁定、判定并仅更新目标报名。 */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId, String userId) {
        try {
            // A1：确认赛事存在、已激活且已设置当前轮次。
            TournamentData tournament = requireEligibleTournament(tournamentId);

            // A2：只按赛事+用户自然键锁定目标报名，不扩展至搭档。
            TournamentEntryState entry = entryPersistence
                    .findByTournamentAndUserForUpdate(tournamentId, userId);
            if (entry == null) {
                throw new BusinessException(BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
            }

            // A3：只查询目标用户在本赛事的进行中比赛参与事实。
            boolean inActiveMatch = matchRepository
                    .existsActiveMatchByTournamentAndUser(tournamentId, userId);

            // A4：使用单人领域契约分档判定，不传递 entryNo 或搭档事实。
            SingleEntryEliminationDecision decision = eliminationService.evaluate(
                    tournament.getCurrentRound(),
                    toSnapshot(entry),
                    inActiveMatch);
            requireEligible(decision);

            // A5：保存前再次复核在赛关系，C11 再条件守护状态与轮次。
            if (matchRepository.existsActiveMatchByTournamentAndUser(
                    tournamentId, userId)) {
                throw new BusinessException(BizErrorCode.TOURNAMENT_ENTRY_IN_ACTIVE_MATCH);
            }
            TournamentEntry.restore(entry).eliminateUnmatched(
                    TournamentEntryRound.valueOf(tournament.getCurrentRound().name()),
                    entryPersistence);
            // A6：事务提交后无业务数据返回。
        } catch (BusinessException exception) {
            throw exception;
        } catch (TournamentEntryDomainException exception) {
            throw toBusinessException(exception);
        } catch (RuntimeException exception) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "淘汰未入赛报名失败");
        }
    }

    private TournamentData requireEligibleTournament(String tournamentId) {
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        if (tournament == null) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_NOT_FOUND);
        }
        if (tournament.getStatus() != TournamentStatusEnum.ACTIVE
                || tournament.getCurrentRound() == null) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_STATUS_INVALID);
        }
        return tournament;
    }

    private SingleEntrySnapshot toSnapshot(TournamentEntryState entry) {
        return new SingleEntrySnapshot(
                entry.userId(),
                TournamentEntryStatusEnum.valueOf(entry.status().name()),
                TournamentRoundEnum.valueOf(entry.currentRound().name()));
    }

    private void requireEligible(SingleEntryEliminationDecision decision) {
        if (decision == SingleEntryEliminationDecision.ELIGIBLE) {
            return;
        }
        if (decision == SingleEntryEliminationDecision.ENTRY_STATUS_OR_ROUND_INVALID) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_ENTRY_STATUS_INVALID);
        }
        if (decision == SingleEntryEliminationDecision.IN_ACTIVE_MATCH) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_ENTRY_IN_ACTIVE_MATCH);
        }
        throw new BusinessException(BizErrorCode.OPERATION_FAILED, "未入赛报名判定上下文无效");
    }

    private BusinessException toBusinessException(
            TournamentEntryDomainException exception) {
        if (TournamentEntry.TOURNAMENT_ENTRY_VERSION_CONFLICT.equals(
                exception.getErrorIdentifier())) {
            return new BusinessException(
                    BizErrorCode.TOURNAMENT_ENTRY_VERSION_CONFLICT,
                    exception.getMessage());
        }
        return new BusinessException(BizErrorCode.OPERATION_FAILED, exception.getMessage());
    }
}
