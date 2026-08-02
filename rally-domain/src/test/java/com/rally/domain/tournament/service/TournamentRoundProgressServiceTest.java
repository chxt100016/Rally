package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.model.TournamentData;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TournamentRoundProgressServiceTest {

    private final TournamentRoundProgressService service = new TournamentRoundProgressService(null, null);

    @Test
    public void shouldNotAdvanceBeforeQualifierCompletes() {
        TournamentData tournament = tournament(16, 0);

        assertNull(service.calculateTargetRound(tournament, round -> 0));
    }

    @Test
    public void shouldStayInQualifierUntilAllSlotsArePaid() {
        TournamentData tournament = tournament(16, 15);

        TournamentRoundEnum target = service.calculateTargetRound(tournament,
                round -> round == TournamentRoundEnum.QUALIFIER ? 16 : 0);

        assertEquals(TournamentRoundEnum.QUALIFIER, target);
    }

    @Test
    public void shouldEnterFirstMainRoundAfterAllSlotsArePaid() {
        TournamentData tournament = tournament(16, 16);

        TournamentRoundEnum target = service.calculateTargetRound(tournament,
                round -> round == TournamentRoundEnum.QUALIFIER ? 16 : 0);

        assertEquals(TournamentRoundEnum.ROUND_16, target);
    }

    @Test
    public void shouldAdvanceAfterAllMatchesInMainRoundComplete() {
        TournamentData tournament = tournament(16, 16);
        Map<TournamentRoundEnum, Integer> completed = new EnumMap<>(TournamentRoundEnum.class);
        completed.put(TournamentRoundEnum.QUALIFIER, 16);
        completed.put(TournamentRoundEnum.ROUND_16, 8);

        TournamentRoundEnum target = service.calculateTargetRound(tournament,
                round -> completed.getOrDefault(round, 0));

        assertEquals(TournamentRoundEnum.ROUND_8, target);
    }

    private TournamentData tournament(int totalSlots, int currentFilledSlots) {
        TournamentData data = new TournamentData();
        data.setTotalSlots(totalSlots);
        data.setCurrentFilledSlots(currentFilledSlots);
        return data;
    }
}
