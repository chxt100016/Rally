package com.rally.domain.tournament.roundprogress;

import com.rally.domain.tournament.enums.TournamentRoundEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable outcome of evaluating one tournament progress snapshot. */
public final class RoundProgressResult {

    private final RoundProgressDecision decision;
    private final TournamentRoundEnum currentRound;
    private final TournamentRoundEnum targetRound;
    private final RoundProgressReason reason;
    private final RoundProgressRejection rejection;
    private final List<RoundProgressEvidence> evidence;

    private RoundProgressResult(
            RoundProgressDecision decision,
            TournamentRoundEnum currentRound,
            TournamentRoundEnum targetRound,
            RoundProgressReason reason,
            RoundProgressRejection rejection,
            List<RoundProgressEvidence> evidence) {
        this.decision = Objects.requireNonNull(decision, "decision");
        this.currentRound = currentRound;
        this.targetRound = targetRound;
        this.reason = reason;
        this.rejection = rejection;
        this.evidence = Collections.unmodifiableList(new ArrayList<>(evidence));
    }

    public static RoundProgressResult accepted(
            RoundProgressDecision decision,
            TournamentRoundEnum currentRound,
            TournamentRoundEnum targetRound,
            RoundProgressReason reason,
            List<RoundProgressEvidence> evidence) {
        return new RoundProgressResult(
                decision,
                currentRound,
                targetRound,
                Objects.requireNonNull(reason, "reason"),
                null,
                evidence);
    }

    public static RoundProgressResult rejected(RoundProgressRejection rejection) {
        return new RoundProgressResult(
                RoundProgressDecision.NOT_READY,
                null,
                null,
                null,
                Objects.requireNonNull(rejection, "rejection"),
                List.of());
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public RoundProgressDecision getDecision() {
        return decision;
    }

    public TournamentRoundEnum getCurrentRound() {
        return currentRound;
    }

    public TournamentRoundEnum getTargetRound() {
        return targetRound;
    }

    public RoundProgressReason getReason() {
        return reason;
    }

    public RoundProgressRejection getRejection() {
        return rejection;
    }

    public List<RoundProgressEvidence> getEvidence() {
        return evidence;
    }
}
