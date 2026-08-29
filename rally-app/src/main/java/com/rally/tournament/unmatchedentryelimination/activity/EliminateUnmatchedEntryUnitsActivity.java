package com.rally.tournament.unmatchedentryelimination.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.entry.TournamentEntry;
import com.rally.domain.tournament.entry.TournamentEntryDomainException;
import com.rally.domain.tournament.entry.TournamentEntryPersistence;
import com.rally.domain.tournament.entry.TournamentEntryRound;
import com.rally.domain.tournament.entry.TournamentEntryState;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.unmatchedentryelimination.ActiveParticipantSnapshot;
import com.rally.domain.tournament.unmatchedentryelimination.UnmatchedEntryEliminationDecision;
import com.rally.domain.tournament.unmatchedentryelimination.UnmatchedEntryEliminationResult;
import com.rally.domain.tournament.unmatchedentryelimination.UnmatchedEntryEliminationService;
import com.rally.domain.tournament.unmatchedentryelimination.UnmatchedEntrySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务活动 eliminate-unmatched-entry-units：整组淘汰当前轮次未进入在途比赛的报名。
 */
@Component
@RequiredArgsConstructor
public class EliminateUnmatchedEntryUnitsActivity {

    private static final Set<TournamentMatchStatusEnum> IN_PROGRESS_STATUSES =
            EnumSet.of(
                    TournamentMatchStatusEnum.MATCHED,
                    TournamentMatchStatusEnum.BOOKING,
                    TournamentMatchStatusEnum.SCHEDULED,
                    TournamentMatchStatusEnum.PENDING_PLAY,
                    TournamentMatchStatusEnum.PENDING_CONFIRM);

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final TournamentMatchRepository matchRepository;
    private final TournamentEntryPersistence entryPersistence;
    private final UnmatchedEntryEliminationService eliminationService;

    /**
     * 候选判定、最新事实复核和全部 C11 条件更新在同一事务中完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId) {
        try {
            // A1：固定本次决策使用的赛制和当前轮次。
            TournamentData tournament = requireEligibleTournament(tournamentId);
            Snapshot initial = loadSnapshot(tournamentId);

            // A2-A3：单双打完整性、状态、轮次和在途占用规则只在领域服务内判定。
            UnmatchedEntryEliminationResult decision = eliminationService.evaluate(
                    tournament.getMatchType(),
                    tournament.getCurrentRound(),
                    initial.entries(),
                    initial.activeParticipants());
            requireAccepted(decision);
            if (decision.getCandidateEntryNos().isEmpty()) {
                return;
            }

            // A4：复核决策上下文和全量最新快照；任一原候选失效均是并发冲突。
            verifyLatestDecision(tournamentId, tournament, decision);
            eliminateCandidates(
                    tournamentId,
                    tournament.getCurrentRound(),
                    decision.getCandidateEntryNos(),
                    initial.entries());
            // A5：事务正常提交，不返回数量或名单。
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
        if (tournament.getStatus() != TournamentStatusEnum.ACTIVE) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
        }
        if (tournament.getCurrentRound() == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "赛事当前轮次不能为空");
        }
        return tournament;
    }

    private Snapshot loadSnapshot(String tournamentId) {
        List<UnmatchedEntrySnapshot> entries = entryRepository
                .findByTournamentId(tournamentId)
                .stream()
                .map(this::toEntrySnapshot)
                .toList();
        List<String> activeMatchIds = matchRepository
                .findByTournamentId(tournamentId)
                .stream()
                .filter(match -> match != null
                        && IN_PROGRESS_STATUSES.contains(match.getStatus()))
                .map(TournamentMatchData::getBizId)
                .filter(matchId -> matchId != null && !matchId.isBlank())
                .distinct()
                .toList();
        List<ActiveParticipantSnapshot> activeParticipants = matchRepository
                .findParticipantsByMatchIds(activeMatchIds)
                .stream()
                .map(this::toActiveParticipantSnapshot)
                .toList();
        return new Snapshot(entries, activeParticipants);
    }

    private void verifyLatestDecision(
            String tournamentId,
            TournamentData expectedTournament,
            UnmatchedEntryEliminationResult expectedDecision) {
        TournamentData latestTournament = tournamentRepository.findByBizId(tournamentId);
        if (latestTournament == null
                || latestTournament.getStatus() != TournamentStatusEnum.ACTIVE
                || latestTournament.getCurrentRound() != expectedTournament.getCurrentRound()
                || latestTournament.getMatchType() != expectedTournament.getMatchType()) {
            throw versionConflict("赛事状态、赛制或当前轮次已变更");
        }

        Snapshot latest = loadSnapshot(tournamentId);
        UnmatchedEntryEliminationResult latestDecision = eliminationService.evaluate(
                expectedTournament.getMatchType(),
                expectedTournament.getCurrentRound(),
                latest.entries(),
                latest.activeParticipants());
        requireAccepted(latestDecision);
        if (!latestDecision.getCandidateEntryNos()
                .containsAll(expectedDecision.getCandidateEntryNos())) {
            throw versionConflict("候选报名或在途比赛关系已变更");
        }
    }

    private void eliminateCandidates(
            String tournamentId,
            com.rally.domain.tournament.enums.TournamentRoundEnum expectedRound,
            List<Integer> candidateEntryNos,
            List<UnmatchedEntrySnapshot> initialEntries) {
        Map<Integer, List<UnmatchedEntrySnapshot>> entriesByNo = initialEntries.stream()
                .filter(entry -> entry != null && entry.entryNo() != null)
                .collect(Collectors.groupingBy(UnmatchedEntrySnapshot::entryNo));
        TournamentEntryRound aggregateRound = TournamentEntryRound.valueOf(expectedRound.name());

        for (Integer entryNo : candidateEntryNos) {
            List<UnmatchedEntrySnapshot> members = entriesByNo.getOrDefault(entryNo, List.of());
            for (UnmatchedEntrySnapshot member : members) {
                TournamentEntryState current = entryPersistence.findByTournamentAndUser(
                        tournamentId, member.userId());
                if (current == null || current.entryNo() != entryNo) {
                    throw versionConflict("候选报名不存在或参赛编号已变更");
                }
                TournamentEntry.restore(current)
                        .eliminateUnmatched(aggregateRound, entryPersistence);
            }
        }
    }

    private void requireAccepted(UnmatchedEntryEliminationResult result) {
        if (result == null
                || result.getDecision()
                != UnmatchedEntryEliminationDecision.ACCEPTED) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "未入赛报名判定上下文无效");
        }
    }

    private UnmatchedEntrySnapshot toEntrySnapshot(TournamentEntryData entry) {
        return new UnmatchedEntrySnapshot(
                entry.getEntryNo(),
                entry.getUserId(),
                entry.getPartnerId(),
                entry.getStatus(),
                entry.getCurrentRound());
    }

    private ActiveParticipantSnapshot toActiveParticipantSnapshot(
            MatchParticipantData participant) {
        return new ActiveParticipantSnapshot(
                participant.getEntryNo(), participant.getUserId());
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

    private BusinessException versionConflict(String message) {
        return new BusinessException(BizErrorCode.TOURNAMENT_ENTRY_VERSION_CONFLICT, message);
    }

    private record Snapshot(
            List<UnmatchedEntrySnapshot> entries,
            List<ActiveParticipantSnapshot> activeParticipants) {
    }
}
