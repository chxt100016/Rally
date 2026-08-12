package com.rally.db.tour.service;

import com.rally.db.tour.entity.TourMatchPO;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TourMatchServiceTest {

    @Test
    public void nullFieldsDoNotOverwriteExistingValues() {
        TourMatchPO existing = match("p1", "p2");
        existing.setCourt("Centre Court");
        existing.setScheduledAt(LocalDateTime.of(2026, 8, 12, 10, 0));
        existing.setSetsJson("[{\"setNumber\":1,\"p1Games\":6,\"p2Games\":3}]");

        TourMatchPO incoming = identityOnly();
        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("Centre Court", merged.getCourt());
        assertEquals(LocalDateTime.of(2026, 8, 12, 10, 0), merged.getScheduledAt());
        assertEquals("[{\"setNumber\":1,\"p1Games\":6,\"p2Games\":3}]", merged.getSetsJson());
    }

    @Test
    public void nonNullFieldsAndScoreSnapshotReplaceExistingValues() {
        TourMatchPO existing = match("p1", "p2");
        existing.setCourt("Court 1");
        existing.setSetsJson("old");
        TourMatchPO incoming = match("p1", "p2");
        incoming.setCourt("Court 2");
        incoming.setSetsJson("new");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("Court 2", merged.getCourt());
        assertEquals("new", merged.getSetsJson());
    }

    @Test
    public void winnerOutsideParticipantsIsRejected() {
        TourMatchPO incoming = match("p1", "p2");
        incoming.setWinnerId("p3");

        assertThrows(IllegalArgumentException.class, () -> TourMatchService.validateWinner(incoming));
    }

    @Test
    public void scoreWithExactlyReversedParticipantsIsNormalized() {
        TourMatchPO existing = match("p1", "p2");
        TourMatchPO incoming = match("p2", "p1");
        incoming.setSetsJson("[{\"setNumber\":1,\"p1Games\":4,\"p2Games\":6,\"p2Tiebreak\":5}]");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("p1", merged.getPlayer1Id());
        assertEquals("p2", merged.getPlayer2Id());
        assertEquals("[{\"p1Games\":6,\"p1Tiebreak\":5,\"p2Games\":4,\"setNumber\":1}]", merged.getSetsJson());
    }

    @Test
    public void scoreWithDifferentParticipantsIsRejected() {
        TourMatchPO existing = match("p1", "p2");
        TourMatchPO incoming = match("p1", "p3");
        incoming.setSetsJson("[]");

        assertThrows(IllegalArgumentException.class, () -> TourMatchService.merge(existing, incoming));
    }

    @Test
    public void completeExternalIdentityIsRequired() {
        TourMatchPO incoming = identityOnly();

        assertThrows(IllegalArgumentException.class, () -> TourMatchService.validateIdentity(incoming));
        incoming.setDrawType("MS");
        TourMatchService.validateIdentity(incoming);
    }

    private TourMatchPO match(String player1Id, String player2Id) {
        TourMatchPO match = identityOnly();
        match.setPlayer1Id(player1Id);
        match.setPlayer2Id(player2Id);
        return match;
    }

    private TourMatchPO identityOnly() {
        TourMatchPO match = new TourMatchPO();
        match.setId(1L);
        match.setDrawId(10L);
        match.setMatchId("MS001");
        match.setTournamentId("806");
        match.setYear(2026);
        return match;
    }
}
