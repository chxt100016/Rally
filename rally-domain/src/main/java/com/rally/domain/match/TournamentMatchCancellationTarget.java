package com.rally.domain.tournament.match;

import java.util.List;

/** C10 持久化端口在自然键锁下返回的最新完整终止目标。 */
public record TournamentMatchCancellationTarget(
        TournamentMatchState state,
        List<TournamentMatchParticipant> participants) {

    public TournamentMatchCancellationTarget {
        participants = List.copyOf(participants);
    }
}
