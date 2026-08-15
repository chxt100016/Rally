package com.rally.db.tour.service;

import com.rally.db.tour.entity.TourPlayerPO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class TourPlayerServiceTest {

    @Test
    public void nullFieldsDoNotEraseExistingPlayerData() {
        TourPlayerPO existing = player("A001", "ATP");
        existing.setFirstName("Alex");
        existing.setNationality("CHN");
        existing.setRank(8);

        TourPlayerPO incoming = player("A001", "ATP");
        incoming.setPoints(3000);

        TourPlayerPO merged = TourPlayerService.merge(existing, incoming);

        assertEquals("Alex", merged.getFirstName());
        assertEquals("CHN", merged.getNationality());
        assertEquals(Integer.valueOf(8), merged.getRank());
        assertEquals(Integer.valueOf(3000), merged.getPoints());
        assertNull(merged.getLastName());
    }

    @Test
    public void playerIdentityCannotChangeDuringMerge() {
        TourPlayerPO existing = player("A001", "ATP");
        TourPlayerPO incoming = player("A001", "WTA");

        assertThrows(IllegalArgumentException.class, () -> TourPlayerService.merge(existing, incoming));
    }

    private TourPlayerPO player(String playerId, String tour) {
        TourPlayerPO player = new TourPlayerPO();
        player.setPlayerId(playerId);
        player.setTour(tour);
        return player;
    }
}
