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
    public void scoreAndParticipantsOverwriteWithoutDirectionValidation() {
        TourMatchPO existing = match("315683", "");
        TourMatchPO incoming = match("p1", "p2");
        incoming.setPlayer1Id("315683");
        incoming.setPlayer2Id("327061");
        incoming.setWinnerId("315683");
        incoming.setSetsJson("[{\"p1Games\":7,\"p2Games\":5,\"setNumber\":1}]");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("315683", merged.getPlayer1Id());
        assertEquals("327061", merged.getPlayer2Id());
        assertEquals("315683", merged.getWinnerId());
        assertEquals("[{\"p1Games\":7,\"p2Games\":5,\"setNumber\":1}]", merged.getSetsJson());
    }

    @Test
    public void differentNonBlankParticipantsAlsoOverwrite() {
        TourMatchPO existing = match("p1", "p2");
        TourMatchPO incoming = match("p1", "p3");
        incoming.setSetsJson("[]");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("p1", merged.getPlayer1Id());
        assertEquals("p3", merged.getPlayer2Id());
        assertEquals("[]", merged.getSetsJson());
    }

    @Test
    public void completeExternalIdentityIsRequired() {
        TourMatchPO incoming = identityOnly();
        incoming.setTournamentId(null);

        assertThrows(IllegalArgumentException.class, () -> TourMatchService.validateIdentity(incoming));
        incoming.setTournamentId("806");
        TourMatchService.validateIdentity(incoming);
    }

    @Test
    public void nonBlankStatusAlwaysOverwrites() {
        TourMatchPO existing = match("p1", "p2");
        existing.setStatus("LIVE");
        TourMatchPO incoming = match("p1", "p2");
        incoming.setStatus("PENDING");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("PENDING", merged.getStatus());
    }

    @Test
    public void blankStringsDoNotOverwriteExistingValues() {
        TourMatchPO existing = match("p1", "p2");
        existing.setCourt("Court 1");
        existing.setSetsJson("score");
        TourMatchPO incoming = match("p1", "p2");
        incoming.setCourt(" ");
        incoming.setSetsJson("");

        TourMatchPO merged = TourMatchService.merge(existing, incoming);

        assertEquals("Court 1", merged.getCourt());
        assertEquals("score", merged.getSetsJson());
    }

    @Test
    public void tournamentYearAndMatchIdMustRemainConsistent() {
        TourMatchPO existing = match("p1", "p2");
        TourMatchPO incoming = match("p1", "p2");
        incoming.setTournamentId("9999");

        assertThrows(IllegalArgumentException.class, () -> TourMatchService.merge(existing, incoming));
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
