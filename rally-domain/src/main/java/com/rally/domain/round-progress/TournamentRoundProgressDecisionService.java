package com.rally.domain.tournament.roundprogress;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only tournament round-progress evaluator.
 *
 * <p>This service deliberately returns a recommendation instead of updating
 * {@code rally_tournament.current_round}. The calling activity must ask the
 * tournament aggregate to revalidate and apply an ADVANCE decision.</p>
 */
@Service
public class TournamentRoundProgressDecisionService {

    private static final String COMPLETED = "COMPLETED";
    private static final Set<Integer> SUPPORTED_TOTAL_SLOTS = Set.of(2, 4, 8, 16, 32, 64);

    private final RoundProgressReader reader;

    public TournamentRoundProgressDecisionService(RoundProgressReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    /** Loads one consistent snapshot and returns a decision without writing state. */
    @Transactional(readOnly = true)
    public RoundProgressResult evaluate(String tournamentId) {
        return evaluate(reader.loadSnapshot(tournamentId));
    }

    /** Pure evaluator exposed for callers that already own a consistent snapshot. */
    public RoundProgressResult evaluate(RoundProgressSnapshot snapshot) {
        if (snapshot == null) {
            return RoundProgressResult.rejected(RoundProgressRejection.TOURNAMENT_NOT_FOUND);
        }

        Integer totalSlotsValue = snapshot.totalSlots();
        if (totalSlotsValue == null || !SUPPORTED_TOTAL_SLOTS.contains(totalSlotsValue)) {
            return RoundProgressResult.rejected(RoundProgressRejection.TOTAL_SLOTS_UNSUPPORTED);
        }

        int totalSlots = totalSlotsValue;
        Map<TournamentRoundEnum, Integer> completedCounts = completedCounts(snapshot);
        List<RoundProgressEvidence> evidence = new ArrayList<>();

        int qualifierCompleted = completedCounts.getOrDefault(TournamentRoundEnum.QUALIFIER, 0);
        evidence.add(new RoundProgressEvidence(
                TournamentRoundEnum.QUALIFIER,
                totalSlots,
                qualifierCompleted));
        if (qualifierCompleted < totalSlots) {
            return RoundProgressResult.accepted(
                    RoundProgressDecision.NOT_READY,
                    snapshot.currentRound(),
                    null,
                    RoundProgressReason.QUALIFIER_MATCHES_PENDING,
                    evidence);
        }

        int filledSlots = snapshot.currentFilledSlots() == null ? 0 : snapshot.currentFilledSlots();
        if (filledSlots < totalSlots) {
            TournamentRoundEnum targetRound = TournamentRoundEnum.QUALIFIER;
            return RoundProgressResult.accepted(
                    decision(snapshot.currentRound(), targetRound),
                    snapshot.currentRound(),
                    targetRound,
                    RoundProgressReason.MAIN_SLOTS_PENDING,
                    evidence);
        }

        TournamentRoundEnum targetRound = TournamentRoundEnum.firstMainRound(totalSlots);
        boolean finalCompleted = false;
        for (TournamentRoundEnum round = targetRound; round != null; round = round.nextRound()) {
            int required = round.requiredMatchCount(totalSlots);
            int actual = completedCounts.getOrDefault(round, 0);
            evidence.add(new RoundProgressEvidence(round, required, actual));
            targetRound = round;
            if (actual < required) {
                break;
            }
            if (round == TournamentRoundEnum.FINAL) {
                finalCompleted = true;
                break;
            }
            targetRound = round.nextRound();
        }

        RoundProgressDecision decision = decision(snapshot.currentRound(), targetRound);
        RoundProgressReason reason;
        if (finalCompleted) {
            reason = RoundProgressReason.FINAL_REMAINS;
        } else if (decision == RoundProgressDecision.ADVANCE) {
            reason = RoundProgressReason.ROUND_READY;
        } else {
            reason = RoundProgressReason.ROUND_MATCHES_PENDING;
        }
        return RoundProgressResult.accepted(
                decision,
                snapshot.currentRound(),
                targetRound,
                reason,
                evidence);
    }

    private Map<TournamentRoundEnum, Integer> completedCounts(RoundProgressSnapshot snapshot) {
        Map<TournamentRoundEnum, Integer> completed = new EnumMap<>(TournamentRoundEnum.class);
        for (RoundProgressMatchSnapshot match : snapshot.matches()) {
            if (match == null
                    || match.round() == null
                    || !Objects.equals(snapshot.tournamentId(), match.tournamentId())
                    || !COMPLETED.equals(match.status())) {
                continue;
            }
            completed.merge(match.round(), 1, Integer::sum);
        }
        return completed;
    }

    private RoundProgressDecision decision(
            TournamentRoundEnum currentRound,
            TournamentRoundEnum targetRound) {
        if (targetRound != null
                && (currentRound == null || targetRound.ordinal() > currentRound.ordinal())) {
            return RoundProgressDecision.ADVANCE;
        }
        return RoundProgressDecision.STAY;
    }
}
