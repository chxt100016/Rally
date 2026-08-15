package com.rally.tour.client;

import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TournamentEntry;
import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawMeta;
import com.rally.tour.parser.DrawParams;
import com.rally.tour.parser.DrawResult;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MatchCollectClientTest {

    @Test
    public void upstreamSliceIsHiddenBehindCanonicalResult() {
        MatchCollectResult result = new StubClient()
                .collect(new DrawParams("806", 2026, "ATP"))
                .get(0);

        assertEquals("806", result.getTournamentId());
        assertEquals("MS", result.getDrawTypeCode());
        assertEquals("MS", result.getMatches().get(0).getDrawType());
        assertEquals("ATP", result.getPlayers().get(0).getTour());
        assertNull(result.getMatches().get(0).getDrawId());
        assertNull(result.getEntries().get(0).getDrawId());
    }

    private static class StubClient extends AbstractMatchCollectClient<String, String> {

        @Override
        protected String request(DrawParams params) {
            return "upstream";
        }

        @Override
        protected List<DrawResult<String>> ms(String data, DrawParams params) {
            return List.of(new DrawResult<>(data, Discipline.SINGLES, "MS",
                    new DrawMeta(32, 5), params.getTournamentId(), params.getYear()));
        }

        @Override
        public List<Match> getMatches(DrawResult<String> draw, String tournamentId) {
            Match match = new Match();
            match.setMatchId("MS001");
            match.setTournamentId(tournamentId);
            return List.of(match);
        }

        @Override
        public List<Player> getPlayers(DrawResult<String> draw) {
            Player player = new Player();
            player.setPlayerId("A001");
            return List.of(player);
        }

        @Override
        public List<TournamentEntry> getEntries(DrawResult<String> draw) {
            TournamentEntry entry = new TournamentEntry();
            entry.setPlayerId("A001");
            return List.of(entry);
        }

        @Override
        public CollectType collectType() {
            return CollectType.ATP_DRAW;
        }
    }
}
