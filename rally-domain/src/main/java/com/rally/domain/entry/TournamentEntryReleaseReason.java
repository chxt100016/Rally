package com.rally.domain.tournament.entry;

/** C5 可以把仍在比赛中的报名释放回匹配池的事实类型。 */
public enum TournamentEntryReleaseReason {
    MATCH_REJECTED,
    MATCH_TIMEOUT,
    UNBOOKED_CANCELLED,
    ADMIN_CANCELLED
}
