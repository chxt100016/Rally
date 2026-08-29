package com.rally.domain.tournament.match;

/** C10 后台运营按赛事内自然键取消指定比赛。 */
public record CancelTournamentMatchCommand(String tournamentId, int matchNo) {
}
