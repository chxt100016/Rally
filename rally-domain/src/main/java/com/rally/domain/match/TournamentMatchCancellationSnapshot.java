package com.rally.domain.tournament.match;

import java.util.List;

/** C10 在软终止前生成的不可变赛约与报名联动快照。 */
public record TournamentMatchCancellationSnapshot(
        String tournamentId,
        String matchId,
        int matchNo,
        String meetupId,
        List<TournamentMatchCancellationParticipant> participants) {

    public TournamentMatchCancellationSnapshot {
        participants = List.copyOf(participants);
    }
}
