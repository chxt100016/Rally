package com.rally.domain.tournament.unmatchedentryelimination;

import java.util.List;
import java.util.Objects;

/** Immutable accepted or rejected elimination decision. */
public final class UnmatchedEntryEliminationResult {

    private final UnmatchedEntryEliminationDecision decision;
    private final List<Integer> candidateEntryNos;
    private final List<Integer> excludedEntryNos;

    private UnmatchedEntryEliminationResult(
            UnmatchedEntryEliminationDecision decision,
            List<Integer> candidateEntryNos,
            List<Integer> excludedEntryNos) {
        this.decision = Objects.requireNonNull(decision, "decision");
        this.candidateEntryNos = List.copyOf(candidateEntryNos);
        this.excludedEntryNos = List.copyOf(excludedEntryNos);
    }

    public static UnmatchedEntryEliminationResult accepted(
            List<Integer> candidateEntryNos,
            List<Integer> excludedEntryNos) {
        return new UnmatchedEntryEliminationResult(
                UnmatchedEntryEliminationDecision.ACCEPTED,
                candidateEntryNos,
                excludedEntryNos);
    }

    public static UnmatchedEntryEliminationResult rejectedInputInvalid() {
        return new UnmatchedEntryEliminationResult(
                UnmatchedEntryEliminationDecision.REJECTED_INPUT_INVALID,
                List.of(),
                List.of());
    }

    public UnmatchedEntryEliminationDecision getDecision() {
        return decision;
    }

    public List<Integer> getCandidateEntryNos() {
        return candidateEntryNos;
    }

    public List<Integer> getExcludedEntryNos() {
        return excludedEntryNos;
    }
}
