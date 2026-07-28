package com.rally.domain.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TournamentGroupData {
    private TournamentData representative;
    private List<TournamentData> tournaments;

    public List<String> getTournamentIds() {
        return tournaments.stream().map(TournamentData::getTournamentId).filter(tournamentId -> tournamentId != null && !tournamentId.isBlank()).distinct().toList();
    }
}
