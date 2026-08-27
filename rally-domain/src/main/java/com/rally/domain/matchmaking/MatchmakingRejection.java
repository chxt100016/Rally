package com.rally.domain.tournament.matchmaking;

/** Stable rejection reasons for structurally invalid matchmaking input. */
public enum MatchmakingRejection {
    REQUEST_REQUIRED,
    ROUND_REQUIRED,
    GROUP_SIZE_UNSUPPORTED,
    CANDIDATES_REQUIRED,
    CANDIDATE_INVALID,
    CANDIDATE_DUPLICATED,
    CANDIDATE_INCOMPLETE,
    CANDIDATE_NOT_WAITING,
    CANDIDATE_ROUND_MISMATCH,
    COMPLETED_PAIRING_INVALID,
    MANUAL_GROUP_INVALID
}
