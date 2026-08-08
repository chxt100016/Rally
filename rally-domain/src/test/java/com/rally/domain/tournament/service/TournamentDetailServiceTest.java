package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class TournamentDetailServiceTest {

    private final TournamentDetailService tournamentDetailService = new TournamentDetailService(null, null, null, null);

    @Test
    public void shouldReturnFrozenActionForFrozenEntry() {
        TournamentData tournament = new TournamentData();
        TournamentEntryData entry = new TournamentEntryData();
        entry.setStatus(TournamentEntryStatusEnum.FROZEN);

        TournamentActionStateEnum actionState = tournamentDetailService.calculateActionState(tournament, entry, null, "user-1");

        assertEquals(TournamentActionStateEnum.FROZEN, actionState);
    }

    @Test
    public void shouldReturnEndBeforeFrozenWhenTournamentEnded() {
        TournamentData tournament = new TournamentData();
        tournament.setEndTime(LocalDateTime.now().minusMinutes(1));
        TournamentEntryData entry = new TournamentEntryData();
        entry.setStatus(TournamentEntryStatusEnum.FROZEN);

        TournamentActionStateEnum actionState = tournamentDetailService.calculateActionState(tournament, entry, null, "user-1");

        assertEquals(TournamentActionStateEnum.END, actionState);
    }
}
