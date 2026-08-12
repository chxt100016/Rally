package com.rally.tour;

import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PlayerTournamentQueryServiceTest {

    @Test
    public void formatsScoreInRequestedPlayerDirection() {
        MatchData match = new MatchData();
        match.setStatus("FINISHED");
        match.setPlayer1Id("p1");
        match.setPlayer2Id("p2");
        match.setSets(List.of(
                SetScore.builder().setNumber(1).p1Games(6).p2Games(3).build(),
                SetScore.builder().setNumber(2).p1Games(4).p2Games(6).build()));

        assertEquals("6-3 4-6", PlayerTournamentQueryService.formatScore(match, "p1"));
        assertEquals("3-6 6-4", PlayerTournamentQueryService.formatScore(match, "p2"));
    }

    @Test
    public void finishedMatchWithoutJsonHasNoDetailedScore() {
        MatchData match = new MatchData();
        match.setStatus("FINISHED");
        match.setPlayer1Id("p1");
        match.setPlayer2Id("p2");

        assertEquals("已完成", PlayerTournamentQueryService.formatScore(match, "p1"));
    }
}
