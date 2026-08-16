package com.rally.domain.tour;

import com.rally.domain.tour.model.MatchQueryVO;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TourMatchQueryDomainServiceTest {

    @Test
    public void upcomingCourtMatchesPutWinnersFirstAndKeepScheduledOrderWithinEachGroup() {
        MatchQueryVO pendingEarly = match("pending-early", null, "2026-08-16T10:00:00");
        MatchQueryVO finishedLate = match("finished-late", "p1", "2026-08-16T12:00:00");
        MatchQueryVO pendingLate = match("pending-late", null, "2026-08-16T13:00:00");
        MatchQueryVO finishedEarly = match("finished-early", "p2", "2026-08-16T09:00:00");

        List<MatchQueryVO> matches = new ArrayList<>(List.of(
                pendingEarly, finishedLate, pendingLate, finishedEarly));
        matches.sort(TourMatchQueryDomainService.UPCOMING_COURT_MATCH_COMPARATOR);

        assertEquals(List.of("finished-early", "finished-late", "pending-early", "pending-late"),
                matches.stream().map(MatchQueryVO::getId).toList());
    }

    @Test
    public void upcomingCourtMatchesPutMissingScheduledTimeLastWithinWinnerGroup() {
        MatchQueryVO missingTime = match("missing-time", "p1", null);
        MatchQueryVO scheduled = match("scheduled", "p2", "2026-08-16T09:00:00");

        List<MatchQueryVO> matches = new ArrayList<>(List.of(missingTime, scheduled));
        matches.sort(TourMatchQueryDomainService.UPCOMING_COURT_MATCH_COMPARATOR);

        assertEquals(List.of("scheduled", "missing-time"),
                matches.stream().map(MatchQueryVO::getId).toList());
    }

    private MatchQueryVO match(String id, String winnerId, String scheduledAt) {
        MatchQueryVO match = new MatchQueryVO();
        match.setId(id);
        match.setWinnerId(winnerId);
        match.setScheduledAt(scheduledAt == null ? null : LocalDateTime.parse(scheduledAt));
        return match;
    }
}
