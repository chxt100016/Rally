package com.rally.domain.tour;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TourTournamentQueryDomainServiceTest {

    private final TourTournamentQueryDomainService service = new TourTournamentQueryDomainService();

    @Test
    public void testGroupByCityAndOverlappingDateAndSortByCategory() {
        TournamentData atp500 = tournament("atp-500", "Hangzhou", "500", "ATP", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 26));
        TournamentData wta1000 = tournament("wta-1000", " hangzhou ", "1000", "WTA", LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 2));
        TournamentData grandSlam = tournament("gs", "London", "GS", "ATP", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 2));
        TournamentData atp250 = tournament("atp-250", "Beijing", "250", "ATP", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 26));

        List<TournamentGroupData> groups = service.groupAndSortTournaments(List.of(atp250, atp500, grandSlam, wta1000));

        assertEquals(3, groups.size());
        assertEquals("gs", groups.get(0).getRepresentative().getTournamentId());
        assertEquals("wta-1000", groups.get(1).getRepresentative().getTournamentId());
        assertEquals(List.of("wta-1000", "atp-500"), groups.get(1).getTournamentIds());
        assertEquals("atp-250", groups.get(2).getRepresentative().getTournamentId());
    }

    @Test
    public void testOverlappingGroupIsTransitive() {
        TournamentData first = tournament("first", "Paris", "500", "ATP", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));
        TournamentData second = tournament("second", "Paris", "500", "WTA", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3));
        TournamentData third = tournament("third", "Paris", "500", "ATP", LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 4));

        List<TournamentGroupData> groups = service.groupAndSortTournaments(List.of(first, second, third));

        assertEquals(1, groups.size());
        assertEquals(List.of("first", "second", "third"), groups.get(0).getTournamentIds());
    }

    @Test
    public void testDifferentCityOrMissingDateDoesNotMerge() {
        TournamentData paris = tournament("paris", "Paris", "500", "ATP", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7));
        TournamentData london = tournament("london", "London", "500", "WTA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7));
        TournamentData missingDate = tournament("missing", "Paris", "500", "WTA", null, LocalDate.of(2026, 7, 7));

        List<TournamentGroupData> groups = service.groupAndSortTournaments(List.of(paris, london, missingDate));

        assertEquals(3, groups.size());
    }

    @Test
    public void testUnknownCategorySortsAfterConfiguredCategories() {
        TournamentData unknown = tournament("unknown", "Unknown", "FINALS", "ATP", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7));
        TournamentData category250 = tournament("250", "Beijing", "250", "ATP", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7));

        List<TournamentGroupData> groups = service.groupAndSortTournaments(List.of(unknown, category250));

        assertEquals("250", groups.get(0).getRepresentative().getTournamentId());
        assertEquals("unknown", groups.get(1).getRepresentative().getTournamentId());
    }

    private TournamentData tournament(String tournamentId, String city, String category, String tour, LocalDate startDate, LocalDate endDate) {
        TournamentData data = new TournamentData();
        data.setTournamentId(tournamentId);
        data.setCity(city);
        data.setCategory(category);
        data.setTour(tour);
        data.setStartDate(startDate);
        data.setEndDate(endDate);
        return data;
    }
}
