package com.rally.domain.tournament.match;

import java.time.LocalDateTime;
import java.util.List;

/** C1 创建匹配比赛的完整事实。 */
public record CreateTournamentMatchCommand(
        String tournamentId,
        int matchNo,
        TournamentMatchRound round,
        int groupSize,
        List<CreateTournamentMatchParticipant> participants,
        LocalDateTime matchedTime,
        String uniqueCourtBookerId) {
}
