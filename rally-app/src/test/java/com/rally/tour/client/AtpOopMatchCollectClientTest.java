package com.rally.tour.client;

import com.rally.client.tourtv.model.AtpOopResponse;
import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.parser.DrawMeta;
import com.rally.tour.parser.DrawParams;
import com.rally.tour.parser.DrawResult;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AtpOopMatchCollectClientTest {

    @Test
    public void consecutiveFollowedByMatchesUsePreviousCalculatedTime() {
        AtpOopResponse tournament = new AtpOopResponse();
        tournament.setOop(List.of(dayWithMatches(
                match("MS001", "10:00+0800", null),
                match("MS002", "10:00+0800", "Followed By"),
                match("MS003", "10:00+0800", "Followed By")
        )));
        DrawResult<AtpOopResponse> draw = new DrawResult<>(
                tournament, Discipline.SINGLES, "MS", new DrawMeta(32, null), "123", 2026);

        List<Match> matches = new AtpOopMatchCollectClient().getMatches(draw, "123");

        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 0), matches.get(0).getScheduledAt());
        assertEquals(LocalDateTime.of(2026, 8, 15, 11, 40), matches.get(1).getScheduledAt());
        assertEquals(LocalDateTime.of(2026, 8, 15, 13, 20), matches.get(2).getScheduledAt());
    }

    @Test
    public void completeCourtOrderIsCalculatedBeforeFilteringMensSingles() {
        AtpOopResponse tournament = tournamentWithMatches(
                match("MS001", "10:00+0800", null),
                match("MD001", "10:00+0800", "Followed By"),
                match("MS002", "10:00+0800", "Followed By")
        );

        List<Match> matches = new StubAtpOopMatchCollectClient(tournament)
                .collect(new DrawParams("123", 2026, "ATP"))
                .get(0)
                .getMatches();

        assertEquals(2, matches.size());
        assertEquals("MS001", matches.get(0).getMatchId());
        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 0), matches.get(0).getScheduledAt());
        assertEquals("MS002", matches.get(1).getMatchId());
        assertEquals(LocalDateTime.of(2026, 8, 15, 13, 20), matches.get(1).getScheduledAt());
    }

    private AtpOopResponse tournamentWithMatches(AtpOopResponse.MatchDetail... matches) {
        AtpOopResponse tournament = new AtpOopResponse();
        tournament.setId(123);
        tournament.setYear(2026);
        tournament.setOop(List.of(dayWithMatches(matches)));
        return tournament;
    }

    private AtpOopResponse.OopDay dayWithMatches(AtpOopResponse.MatchDetail... matches) {
        AtpOopResponse.CourtDetail court = new AtpOopResponse.CourtDetail();
        court.setMatches(List.of(matches));

        LinkedHashMap<String, AtpOopResponse.CourtDetail> courts = new LinkedHashMap<>();
        courts.put("center", court);

        AtpOopResponse.OopDay day = new AtpOopResponse.OopDay();
        day.setCourts(courts);
        return day;
    }

    private AtpOopResponse.MatchDetail match(String matchId, String isoTime, String timeText) {
        AtpOopResponse.MatchDetail match = new AtpOopResponse.MatchDetail();
        match.setMatchId(matchId);
        match.setAssociationCode("ATP");
        match.setMatchDate("2026-08-15");
        match.setNotBeforeISOTime(isoTime);
        match.setNotBeforeText(timeText);
        return match;
    }

    private static class StubAtpOopMatchCollectClient extends AtpOopMatchCollectClient {
        private final AtpOopResponse tournament;

        private StubAtpOopMatchCollectClient(AtpOopResponse tournament) {
            this.tournament = tournament;
        }

        @Override
        protected List<AtpOopResponse> request(DrawParams params) {
            return List.of(tournament);
        }
    }
}
