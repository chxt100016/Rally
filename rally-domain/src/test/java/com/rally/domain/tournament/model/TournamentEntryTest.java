package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TournamentEntryTest {

    @Test
    public void shouldMoveMainDrawWinnerToNextRound() {
        TournamentEntryData data = entryData(TournamentEntryStageEnum.MAIN, TournamentRoundEnum.ROUND_16);

        new TournamentEntry(data).advanceAfterWin(TournamentRoundEnum.ROUND_16);

        assertEquals(TournamentEntryStatusEnum.WAITING, data.getStatus());
        assertEquals(TournamentRoundEnum.ROUND_8, data.getCurrentRound());
    }

    @Test
    public void shouldMoveQualifierWinnerToPayingWithoutChangingRound() {
        TournamentEntryData data = entryData(TournamentEntryStageEnum.QUALIFY, TournamentRoundEnum.QUALIFIER);

        new TournamentEntry(data).advanceAfterWin(TournamentRoundEnum.QUALIFIER);

        assertEquals(TournamentEntryStatusEnum.PAYING, data.getStatus());
        assertEquals(TournamentRoundEnum.QUALIFIER, data.getCurrentRound());
    }

    @Test
    public void shouldKeepChampionInFinal() {
        TournamentEntryData data = entryData(TournamentEntryStageEnum.MAIN, TournamentRoundEnum.FINAL);

        new TournamentEntry(data).advanceAfterWin(TournamentRoundEnum.FINAL);

        assertEquals(TournamentEntryStatusEnum.WAITING, data.getStatus());
        assertEquals(TournamentRoundEnum.FINAL, data.getCurrentRound());
    }

    private TournamentEntryData entryData(TournamentEntryStageEnum stage, TournamentRoundEnum round) {
        TournamentEntryData data = new TournamentEntryData();
        data.setStage(stage);
        data.setStatus(TournamentEntryStatusEnum.IN_MATCH);
        data.setCurrentRound(round);
        return data;
    }
}
